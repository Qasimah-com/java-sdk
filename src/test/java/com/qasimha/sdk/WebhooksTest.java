package com.qasimha.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.qasimha.sdk.exception.ConfigurationException;
import com.qasimha.sdk.exception.ValidationException;
import com.qasimha.sdk.exception.WebhookSignatureException;
import com.qasimha.sdk.exception.WebhookSignatureException.Reason;
import com.qasimha.sdk.internal.Hex;
import com.qasimha.sdk.model.WebhookEvent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class WebhooksTest {

  private static final JsonNode VECTORS = Fixtures.json("webhook-signature.json");
  private static final String SECRET = VECTORS.get("secret").asText();
  private static final long TIMESTAMP = VECTORS.get("timestamp").asLong();
  private static final Instant WITHIN =
      Instant.ofEpochSecond(VECTORS.get("nowWithinTolerance").asLong());
  private static final Instant OUTSIDE =
      Instant.ofEpochSecond(VECTORS.get("nowOutsideTolerance").asLong());
  private static final Duration TOLERANCE = Duration.ofSeconds(VECTORS.get("tolerance").asLong());

  private static byte[] body(String version) {
    return VECTORS.get(version).get("body").asText().getBytes(StandardCharsets.UTF_8);
  }

  private static Map<String, String> headers(String version) {
    Map<String, String> map = new HashMap<>();
    VECTORS
        .get(version)
        .get("headers")
        .fields()
        .forEachRemaining(e -> map.put(e.getKey(), e.getValue().asText()));
    return map;
  }

  private static WebhookOptions within() {
    return WebhookOptions.defaults().tolerance(TOLERANCE).now(WITHIN);
  }

  private static WebhookOptions outside() {
    return WebhookOptions.defaults().tolerance(TOLERANCE).now(OUTSIDE);
  }

  private static String hmacHex(String secret, byte[] material) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return Hex.encode(mac.doFinal(material));
  }

  @Test
  void vectorFileIsInternallyConsistent() throws Exception {
    assertEquals(
        "sha256=" + hmacHex(SECRET, body("v0")), VECTORS.get("v0").get("signatureHeader").asText());
    byte[] prefixed =
        (TIMESTAMP + "." + new String(body("v1"), StandardCharsets.UTF_8))
            .getBytes(StandardCharsets.UTF_8);
    assertEquals(
        "t=" + TIMESTAMP + ",v1=" + hmacHex(SECRET, prefixed),
        VECTORS.get("v1").get("signatureHeader").asText());
  }

  @Test
  void verifiesTheCurrentFormatAndDecodesTheEvent() {
    Webhooks webhooks = new Webhooks(SECRET);
    WebhookEvent event = webhooks.constructEvent(body("v0"), headers("v0"), null, within());
    assertEquals("evt_0123456789abcdef0123456789abcdef", event.getId());
    assertEquals("order.completed", event.getType());
    assertNull(event.getOrderId());
    assertEquals("ORDER-1", event.getOrderReferenceId());
    assertEquals("Confirmed", event.getStatus());
    assertEquals(0, new BigDecimal("1200.00").compareTo(event.getAmount()));
    assertEquals("SAR", event.getCurrency());
    assertEquals(Instant.parse("2026-09-25T09:00:00Z"), event.getOccurredAt());
    assertEquals("order.completed", event.getRaw().get("eventType"));
  }

  @Test
  void verifiesTheNextFormatAndDecodesTheEvent() {
    Webhooks webhooks = new Webhooks(SECRET);
    WebhookEvent event = webhooks.constructEvent(body("v1"), headers("v1"), null, within());
    assertEquals("evt_1", event.getId());
    assertEquals("order.confirmed", event.getType());
    assertEquals("9a2e6c4b-1111-4c2d-8a1f-000000000001", event.getOrderId());
    assertEquals("ORDER-1", event.getOrderReferenceId());
    assertEquals("Confirmed", event.getStatus());
    assertEquals(0, new BigDecimal("1200.00").compareTo(event.getAmount()));
    assertEquals(Instant.parse("2026-09-25T09:00:00Z"), event.getOccurredAt());
    assertTrue(event.getRaw().containsKey("data"));
  }

  @Test
  void rotatedSecretHeaderWithTwoSignaturesPasses() {
    Webhooks webhooks = new Webhooks(SECRET);
    String rotated = VECTORS.get("v1").get("rotated").asText();
    webhooks.verifySignature(body("v1"), rotated, null, null, within());
    WebhookEvent event =
        webhooks.constructEvent(body("v1"), rotated, String.valueOf(TIMESTAMP), null, within());
    assertEquals("evt_1", event.getId());
  }

  @Test
  void timestampOutsideToleranceIsRejectedInBothFormats() {
    Webhooks webhooks = new Webhooks(SECRET);
    assertEquals(
        Reason.TIMESTAMP_OUTSIDE_TOLERANCE,
        assertThrows(
                WebhookSignatureException.class,
                () -> webhooks.constructEvent(body("v0"), headers("v0"), null, outside()))
            .getReason());
    assertEquals(
        Reason.TIMESTAMP_OUTSIDE_TOLERANCE,
        assertThrows(
                WebhookSignatureException.class,
                () -> webhooks.constructEvent(body("v1"), headers("v1"), null, outside()))
            .getReason());
    assertEquals("timestamp_outside_tolerance", Reason.TIMESTAMP_OUTSIDE_TOLERANCE.getCode());
  }

  @Test
  void currentFormatWithoutTimestampHeaderIsAcceptedRegardlessOfNow() {
    Webhooks webhooks = new Webhooks(SECRET);
    Map<String, String> noTimestamp = headers("v0");
    noTimestamp.remove("X-Qasimha-Timestamp");
    WebhookEvent event = webhooks.constructEvent(body("v0"), noTimestamp, null, outside());
    assertEquals("order.completed", event.getType());
  }

  @Test
  void missingHeader() {
    Webhooks webhooks = new Webhooks(SECRET);
    WebhookSignatureException e =
        assertThrows(
            WebhookSignatureException.class,
            () ->
                webhooks.constructEvent(body("v0"), Map.of("X-Qasimha-Event", "order.completed")));
    assertEquals(Reason.MISSING_HEADER, e.getReason());
    assertEquals("missing_header", e.getCode());
    assertEquals(
        Reason.MISSING_HEADER,
        assertThrows(
                WebhookSignatureException.class,
                () -> webhooks.verifySignature(body("v0"), "  ", SECRET))
            .getReason());
  }

  @Test
  void malformedHeaders() {
    Webhooks webhooks = new Webhooks(SECRET);
    String v1 =
        VECTORS.get("v1").get("signatureHeader").asText().substring("t=1758790800,".length());
    for (String header :
        List.of(
            "sha256=zz",
            "sha256=abc",
            "sha256=",
            "garbage",
            "t=abc," + v1,
            v1,
            "t=" + TIMESTAMP,
            "t=" + TIMESTAMP + ",v1=nothex",
            "t=" + TIMESTAMP + ",t=" + TIMESTAMP + "," + v1,
            "=x")) {
      WebhookSignatureException e =
          assertThrows(
              WebhookSignatureException.class,
              () -> webhooks.verifySignature(body("v1"), header, SECRET, null, within()),
              header);
      assertEquals(Reason.MALFORMED_HEADER, e.getReason(), header);
    }
    WebhookSignatureException badTimestamp =
        assertThrows(
            WebhookSignatureException.class,
            () ->
                webhooks.verifySignature(
                    body("v0"),
                    VECTORS.get("v0").get("signatureHeader").asText(),
                    SECRET,
                    "yesterday",
                    within()));
    assertEquals(Reason.MALFORMED_HEADER, badTimestamp.getReason());
  }

  @Test
  void signatureMismatchOnWrongSecretOrTamperedBody() {
    Webhooks webhooks = new Webhooks("whsec_other");
    assertEquals(
        Reason.SIGNATURE_MISMATCH,
        assertThrows(
                WebhookSignatureException.class,
                () -> webhooks.constructEvent(body("v0"), headers("v0"), null, within()))
            .getReason());
    assertEquals(
        Reason.SIGNATURE_MISMATCH,
        assertThrows(
                WebhookSignatureException.class,
                () -> webhooks.constructEvent(body("v1"), headers("v1"), null, within()))
            .getReason());

    Webhooks right = new Webhooks(SECRET);
    byte[] tampered =
        new String(body("v0"), StandardCharsets.UTF_8)
            .replace("1200.00", "1.00")
            .getBytes(StandardCharsets.UTF_8);
    assertEquals(
        Reason.SIGNATURE_MISMATCH,
        assertThrows(
                WebhookSignatureException.class,
                () -> right.constructEvent(tampered, headers("v0"), null, within()))
            .getReason());
  }

  @Test
  void equalLengthWrongDigestIsAMismatchNotMalformed() {
    Webhooks webhooks = new Webhooks(SECRET);
    String wrong = "sha256=" + "0".repeat(64);
    assertEquals(
        Reason.SIGNATURE_MISMATCH,
        assertThrows(
                WebhookSignatureException.class,
                () -> webhooks.verifySignature(body("v0"), wrong, SECRET))
            .getReason());
    String wrongV1 = "t=" + TIMESTAMP + ",v1=" + "f".repeat(64);
    assertEquals(
        Reason.SIGNATURE_MISMATCH,
        assertThrows(
                WebhookSignatureException.class,
                () -> webhooks.verifySignature(body("v1"), wrongV1, SECRET, null, within()))
            .getReason());
  }

  @Test
  void headerNamesAreCaseInsensitiveAndValuesMayBeLists() {
    Webhooks webhooks = new Webhooks(SECRET);
    Map<String, Object> lower = new HashMap<>();
    headers("v1").forEach((k, v) -> lower.put(k.toLowerCase(), List.of(v)));
    WebhookEvent event = webhooks.constructEvent(body("v1"), lower, null, within());
    assertEquals("evt_1", event.getId());
  }

  @Test
  void stringBodyOverloadUsesUtf8Bytes() {
    Webhooks webhooks =
        new Webhooks(SECRET, java.time.Clock.fixed(WITHIN, java.time.ZoneOffset.UTC));
    WebhookEvent event =
        webhooks.constructEvent(VECTORS.get("v0").get("body").asText(), headers("v0"));
    assertEquals("order.completed", event.getType());
  }

  @Test
  void secretIsRequired() {
    Webhooks none = new Webhooks(null);
    assertThrows(
        ConfigurationException.class, () -> none.constructEvent(body("v0"), headers("v0")));
    WebhookEvent event = none.constructEvent(body("v0"), headers("v0"), SECRET, within());
    assertEquals("order.completed", event.getType());
  }

  @Test
  void clientWebhooksUseTheConfiguredSecret() {
    QasimhaClient client =
        QasimhaClient.builder()
            .apiKey(Fixtures.TEST_KEY)
            .webhookSecret(SECRET)
            .clock(java.time.Clock.fixed(WITHIN, java.time.ZoneOffset.UTC))
            .build();
    WebhookEvent event = client.webhooks().constructEvent(body("v1"), headers("v1"));
    assertEquals("order.confirmed", event.getType());
  }

  @Test
  void validSignatureOverNonJsonBodyIsAValidationError() throws Exception {
    byte[] notJson = "not json".getBytes(StandardCharsets.UTF_8);
    String header = "sha256=" + hmacHex(SECRET, notJson);
    Webhooks webhooks = new Webhooks(SECRET);
    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> webhooks.constructEvent(notJson, header, null, null, WebhookOptions.defaults()));
    assertEquals("body", e.getErrors().get(0).getField());
  }

  @Test
  void toleranceMustNotBeNegative() {
    assertThrows(
        ConfigurationException.class,
        () -> WebhookOptions.defaults().tolerance(Duration.ofSeconds(-1)));
    assertEquals(Duration.ofSeconds(300), WebhookOptions.defaults().getTolerance());
  }
}
