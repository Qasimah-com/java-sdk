package com.qasimha.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.qasimha.sdk.exception.ConfigurationException;
import com.qasimha.sdk.exception.ValidationException;
import com.qasimha.sdk.exception.WebhookSignatureException;
import com.qasimha.sdk.exception.WebhookSignatureException.Reason;
import com.qasimha.sdk.internal.Hex;
import com.qasimha.sdk.internal.Json;
import com.qasimha.sdk.model.FieldError;
import com.qasimha.sdk.model.ResponseMeta;
import com.qasimha.sdk.model.WebhookEvent;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verifies webhook deliveries and decodes them. Both signature formats are accepted: {@code
 * sha256=<hex>} over the raw body (with an optional {@code X-Qasimha-Timestamp} replay guard) and
 * {@code t=<unix>,v1=<hex>[,v1=<hex>]} over {@code "<t>." + body}. Every comparison is
 * constant-time and every failure is a {@link WebhookSignatureException} with a reason.
 *
 * <p>Always hand over the raw request bytes exactly as received; a re-serialised body will not
 * verify.
 */
public final class Webhooks {

  /** The signature header. */
  public static final String SIGNATURE_HEADER = "X-Qasimha-Signature";

  /** The timestamp header. */
  public static final String TIMESTAMP_HEADER = "X-Qasimha-Timestamp";

  private static final String HMAC = "HmacSHA256";
  private static final int DIGEST_BYTES = 32;

  private final String defaultSecret;
  private final Clock clock;

  /** A verifier with the given default secret; {@code secret} may be null and passed per call. */
  public Webhooks(String secret) {
    this(secret, Clock.systemUTC());
  }

  Webhooks(String secret, Clock clock) {
    this.defaultSecret = secret;
    this.clock = clock == null ? Clock.systemUTC() : clock;
  }

  /** Verifies with the client's secret and default options, reading the headers from the map. */
  public WebhookEvent constructEvent(byte[] rawBody, Map<String, ?> headers) {
    return constructEvent(rawBody, headers, null, WebhookOptions.defaults());
  }

  /**
   * Verifies a body that the framework handed over as an unmodified string. The bytes are the UTF-8
   * encoding of the string; only use this when no byte-level access exists.
   */
  public WebhookEvent constructEvent(String rawBody, Map<String, ?> headers) {
    return constructEvent(bytes(rawBody), headers, null, WebhookOptions.defaults());
  }

  /**
   * Verifies and decodes a delivery.
   *
   * @param rawBody the request body exactly as received
   * @param headers the request headers; names are matched case-insensitively and values may be
   *     strings or collections of strings
   * @param secret the signing secret, or null to use the client's {@code webhookSecret}
   * @param options tolerance and, for tests, a fixed {@code now}
   */
  public WebhookEvent constructEvent(
      byte[] rawBody, Map<String, ?> headers, String secret, WebhookOptions options) {
    return constructEvent(
        rawBody,
        header(headers, SIGNATURE_HEADER),
        header(headers, TIMESTAMP_HEADER),
        secret,
        options);
  }

  /** Verifies and decodes a delivery from the two header values. */
  public WebhookEvent constructEvent(
      byte[] rawBody,
      String signatureHeader,
      String timestampHeader,
      String secret,
      WebhookOptions options) {
    verifySignature(rawBody, signatureHeader, secret, timestampHeader, options);
    return decode(rawBody);
  }

  /** Verifies a signature with default options; throws on any failure. */
  public void verifySignature(byte[] rawBody, String signatureHeader, String secret) {
    verifySignature(rawBody, signatureHeader, secret, null, WebhookOptions.defaults());
  }

  /**
   * Verifies a signature; throws on any failure.
   *
   * @param timestampHeader the {@code X-Qasimha-Timestamp} value, or null; used as a replay guard
   *     for the {@code sha256=} format
   */
  public void verifySignature(
      byte[] rawBody,
      String signatureHeader,
      String secret,
      String timestampHeader,
      WebhookOptions options) {
    if (rawBody == null) {
      throw new ConfigurationException("rawBody must not be null");
    }
    String key = secret != null ? secret : defaultSecret;
    if (key == null || key.isEmpty()) {
      throw new ConfigurationException("A webhook secret is required to verify a delivery");
    }
    if (signatureHeader == null || signatureHeader.trim().isEmpty()) {
      throw new WebhookSignatureException(
          Reason.MISSING_HEADER, "The " + SIGNATURE_HEADER + " header is missing");
    }
    WebhookOptions opts = options == null ? WebhookOptions.defaults() : options;
    long now = (opts.getNow() != null ? opts.getNow() : clock.instant()).getEpochSecond();
    long tolerance = opts.getTolerance().getSeconds();
    String signature = signatureHeader.trim();

    if (signature.startsWith("sha256=")) {
      byte[] provided = digestHex(signature.substring("sha256=".length()));
      if (timestampHeader != null && !timestampHeader.trim().isEmpty()) {
        long timestamp = parseTimestamp(timestampHeader.trim());
        checkTolerance(timestamp, now, tolerance);
      }
      byte[] expected = hmac(key, rawBody);
      if (!MessageDigest.isEqual(expected, provided)) {
        throw new WebhookSignatureException(
            Reason.SIGNATURE_MISMATCH, "The signature does not match the body");
      }
      return;
    }

    Long timestamp = null;
    List<byte[]> candidates = new ArrayList<>();
    for (String part : signature.split(",")) {
      String element = part.trim();
      int eq = element.indexOf('=');
      if (eq <= 0) {
        throw new WebhookSignatureException(
            Reason.MALFORMED_HEADER, "The signature header is not sha256=... or t=...,v1=...");
      }
      String name = element.substring(0, eq).trim();
      String value = element.substring(eq + 1).trim();
      if (name.equals("t")) {
        if (timestamp != null) {
          throw new WebhookSignatureException(
              Reason.MALFORMED_HEADER, "The signature header carries more than one t");
        }
        timestamp = parseTimestamp(value);
      } else if (name.equals("v1")) {
        candidates.add(digestHex(value));
      }
    }
    if (timestamp == null || candidates.isEmpty()) {
      throw new WebhookSignatureException(
          Reason.MALFORMED_HEADER, "The signature header needs t and at least one v1");
    }
    checkTolerance(timestamp, now, tolerance);
    byte[] prefix = (timestamp + ".").getBytes(StandardCharsets.US_ASCII);
    byte[] material = new byte[prefix.length + rawBody.length];
    System.arraycopy(prefix, 0, material, 0, prefix.length);
    System.arraycopy(rawBody, 0, material, prefix.length, rawBody.length);
    byte[] expected = hmac(key, material);
    boolean matched = false;
    for (byte[] candidate : candidates) {
      matched |= MessageDigest.isEqual(expected, candidate);
    }
    if (!matched) {
      throw new WebhookSignatureException(
          Reason.SIGNATURE_MISMATCH, "No v1 signature matches the body");
    }
  }

  private static WebhookEvent decode(byte[] rawBody) {
    JsonNode root = Json.readTree(rawBody);
    if (root == null || !root.isObject()) {
      throw ValidationException.clientSide(
          Collections.singletonList(
              new FieldError("body", "The webhook body is not a JSON object")));
    }
    return Json.toModel(root, WebhookEvent.class, new ResponseMeta(Json.toMap(root), null, false));
  }

  private static byte[] hmac(String secret, byte[] material) {
    try {
      Mac mac = Mac.getInstance(HMAC);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC));
      return mac.doFinal(material);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new ConfigurationException("HMAC-SHA256 is not available: " + e.getMessage());
    }
  }

  private static byte[] digestHex(String hex) {
    byte[] decoded = Hex.decode(hex.trim());
    if (decoded == null || decoded.length != DIGEST_BYTES) {
      throw new WebhookSignatureException(
          Reason.MALFORMED_HEADER, "The signature is not a 64-character hex digest");
    }
    return decoded;
  }

  private static long parseTimestamp(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      throw new WebhookSignatureException(
          Reason.MALFORMED_HEADER, "The timestamp is not unix seconds");
    }
  }

  private static void checkTolerance(long timestamp, long now, long tolerance) {
    if (Math.abs(now - timestamp) > tolerance) {
      throw new WebhookSignatureException(
          Reason.TIMESTAMP_OUTSIDE_TOLERANCE,
          "The delivery timestamp is outside the " + tolerance + " s tolerance");
    }
  }

  private static String header(Map<String, ?> headers, String name) {
    if (headers == null) {
      return null;
    }
    for (Map.Entry<String, ?> entry : headers.entrySet()) {
      if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
        Object value = entry.getValue();
        if (value instanceof Collection) {
          Collection<?> values = (Collection<?>) value;
          return values.isEmpty() ? null : String.valueOf(values.iterator().next());
        }
        if (value instanceof String[]) {
          String[] values = (String[]) value;
          return values.length == 0 ? null : values[0];
        }
        return value == null ? null : value.toString();
      }
    }
    return null;
  }

  private static byte[] bytes(String rawBody) {
    if (rawBody == null) {
      throw new ConfigurationException("rawBody must not be null");
    }
    return rawBody.getBytes(StandardCharsets.UTF_8);
  }

  /** The clock used when {@link WebhookOptions#getNow()} is unset. */
  Clock clock() {
    return clock;
  }

  /** The {@code Instant} the verifier would use as now. */
  Instant now() {
    return clock.instant();
  }
}
