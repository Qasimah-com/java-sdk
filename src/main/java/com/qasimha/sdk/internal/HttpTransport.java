package com.qasimha.sdk.internal;

import com.qasimha.sdk.LogLevel;
import com.qasimha.sdk.Logger;
import com.qasimha.sdk.RequestOptions;
import com.qasimha.sdk.Sleeper;
import com.qasimha.sdk.exception.ConfigurationException;
import com.qasimha.sdk.exception.ConnectionException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sends requests with the SDK's headers, retries and idempotency rules, and hands back the final
 * response for the resources to interpret.
 */
public final class HttpTransport {

  /** The accept value for JSON endpoints. */
  public static final String ACCEPT_JSON = "application/json";

  /** The accept value for the contract download. */
  public static final String ACCEPT_DOCUMENT = "application/pdf, application/json;q=0.9, */*;q=0.8";

  private static final String API_PREFIX = "/api/v1";
  private static final String API_KEY_HEADER = "X-Api-Key";
  private static final String PUBLIC_KEY_HEADER = "X-Public-Key";

  private final HttpClient client;
  private final String baseUrl;
  private final String apiKey;
  private final String publishableKey;
  private final Duration timeout;
  private final int maxRetries;
  private final String language;
  private final String userAgent;
  private final Logger logger;
  private final Sleeper sleeper;
  private final Clock clock;
  private final Backoff backoff;

  public HttpTransport(
      HttpClient client,
      String baseUrl,
      String apiKey,
      String publishableKey,
      Duration timeout,
      int maxRetries,
      String language,
      String userAgent,
      Logger logger,
      Sleeper sleeper,
      Clock clock,
      Backoff backoff) {
    this.client = client;
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.publishableKey = publishableKey;
    this.timeout = timeout;
    this.maxRetries = maxRetries;
    this.language = language;
    this.userAgent = userAgent;
    this.logger = logger;
    this.sleeper = sleeper;
    this.clock = clock;
    this.backoff = backoff;
  }

  public Clock clock() {
    return clock;
  }

  public Logger logger() {
    return logger;
  }

  /** The client's publishable key, or null when none was configured. */
  public String publishableKey() {
    return publishableKey;
  }

  /** The client's {@code Accept-Language}. */
  public String language() {
    return language;
  }

  public HttpResult get(String path, Map<String, String> query) {
    return send("GET", path, path, query, null, RequestOptions.defaults(), ACCEPT_JSON, null, null);
  }

  /** A GET whose log entry names {@code logPath} instead of the path sent. */
  public HttpResult get(String path, Map<String, String> query, String logPath) {
    return send(
        "GET", path, logPath, query, null, RequestOptions.defaults(), ACCEPT_JSON, null, null);
  }

  /**
   * A GET authenticated with a publishable key: {@code X-Public-Key} is sent and {@code X-Api-Key}
   * is not, and {@code Accept-Language} is {@code acceptLanguage}.
   */
  public HttpResult getPublic(
      String path, Map<String, String> query, String publishableKey, String acceptLanguage) {
    return send(
        "GET",
        path,
        path,
        query,
        null,
        RequestOptions.defaults(),
        ACCEPT_JSON,
        publishableKey,
        acceptLanguage);
  }

  public HttpResult getDocument(String path) {
    return send(
        "GET", path, path, null, null, RequestOptions.defaults(), ACCEPT_DOCUMENT, null, null);
  }

  public HttpResult post(String path, Object body, RequestOptions options) {
    byte[] bytes = body == null ? null : Json.write(body);
    return send("POST", path, path, null, bytes, options, ACCEPT_JSON, null, null);
  }

  public HttpResult put(String path, Object body, RequestOptions options) {
    byte[] bytes = body == null ? null : Json.write(body);
    return send("PUT", path, path, null, bytes, options, ACCEPT_JSON, null, null);
  }

  public HttpResult delete(String path, RequestOptions options) {
    return send("DELETE", path, path, null, null, options, ACCEPT_JSON, null, null);
  }

  private HttpResult send(
      String method,
      String path,
      String logPath,
      Map<String, String> query,
      byte[] body,
      RequestOptions options,
      String accept,
      String publicKey,
      String acceptLanguage) {
    RequestOptions opts = options == null ? RequestOptions.defaults() : options;
    URI uri = buildUri(path, query);
    String correlationId = UUID.randomUUID().toString();
    String idempotencyKey = null;
    if (isMutation(method) && !opts.isIdempotencyDisabled()) {
      idempotencyKey =
          opts.getIdempotencyKey() != null
              ? opts.getIdempotencyKey()
              : UUID.randomUUID().toString();
    }
    boolean retriesAllowed = "GET".equals(method) || idempotencyKey != null;
    int maxAttempts = retriesAllowed ? maxRetries + 1 : 1;
    String authHeader = publicKey != null ? PUBLIC_KEY_HEADER : API_KEY_HEADER;
    String authValue = publicKey != null ? publicKey : apiKey;
    String lang = acceptLanguage != null ? acceptLanguage : language;

    for (int attempt = 1; ; attempt++) {
      HttpRequest request =
          buildRequest(
              method,
              uri,
              body,
              accept,
              correlationId,
              idempotencyKey,
              authHeader,
              authValue,
              lang);
      long started = System.nanoTime();
      HttpResponse<byte[]> response;
      try {
        response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
      } catch (IOException e) {
        long durationMs = elapsedMillis(started);
        ConnectionException failure =
            new ConnectionException("Could not reach the Qasimha API: " + describe(e), e);
        if (attempt < maxAttempts) {
          long delay = backoff.delayMillis(attempt, null);
          log(
              LogLevel.WARN,
              "request failed, retrying",
              fields(method, logPath, null, durationMs, null, attempt, null, delay));
          pause(delay);
          continue;
        }
        log(
            LogLevel.ERROR,
            "request failed",
            fields(method, logPath, null, durationMs, null, attempt, null, null));
        throw failure;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ConnectionException("Interrupted while waiting for the Qasimha API", e);
      }

      long durationMs = elapsedMillis(started);
      HttpResult result =
          new HttpResult(response.statusCode(), response.headers(), response.body(), attempt);
      if (shouldRetry(result, idempotencyKey != null) && attempt < maxAttempts) {
        String retryAfter =
            result.getStatus() == 429 ? result.header("Retry-After").orElse(null) : null;
        long delay = backoff.delayMillis(attempt, retryAfter);
        log(
            LogLevel.WARN,
            "retryable response, retrying",
            fields(
                method,
                logPath,
                result.getStatus(),
                durationMs,
                result.getRequestId(),
                attempt,
                result.getCode(),
                delay));
        pause(delay);
        continue;
      }
      log(
          LogLevel.DEBUG,
          "request completed",
          fields(
              method,
              logPath,
              result.getStatus(),
              durationMs,
              result.getRequestId(),
              attempt,
              result.getCode(),
              null));
      return result;
    }
  }

  private static boolean isMutation(String method) {
    return "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method);
  }

  private boolean shouldRetry(HttpResult result, boolean idempotent) {
    switch (result.getStatus()) {
      case 429:
      case 502:
      case 503:
      case 504:
        return true;
      case 409:
        return idempotent && ErrorMapper.REQUEST_IN_PROGRESS.equals(result.getCode());
      default:
        return false;
    }
  }

  private HttpRequest buildRequest(
      String method,
      URI uri,
      byte[] body,
      String accept,
      String correlationId,
      String idempotencyKey,
      String authHeader,
      String authValue,
      String acceptLanguage) {
    try {
      HttpRequest.Builder builder =
          HttpRequest.newBuilder(uri)
              .timeout(timeout)
              .header(authHeader, authValue)
              .header("Accept", accept)
              .header("Accept-Language", acceptLanguage)
              .header("User-Agent", userAgent)
              .header("X-Correlation-Id", correlationId);
      if (idempotencyKey != null) {
        builder.header("Idempotency-Key", idempotencyKey);
      }
      if ("GET".equals(method)) {
        builder.GET();
      } else if ("DELETE".equals(method)) {
        builder.DELETE();
      } else {
        builder.header("Content-Type", "application/json; charset=utf-8");
        builder.method(
            method,
            body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofByteArray(body));
      }
      return builder.build();
    } catch (IllegalArgumentException e) {
      throw new ConfigurationException("Could not build the request: " + e.getMessage());
    }
  }

  private URI buildUri(String path, Map<String, String> query) {
    StringBuilder url = new StringBuilder(baseUrl).append(API_PREFIX).append(path);
    if (query != null && !query.isEmpty()) {
      char separator = '?';
      for (Map.Entry<String, String> entry : query.entrySet()) {
        if (entry.getValue() == null) {
          continue;
        }
        url.append(separator)
            .append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
            .append('=')
            .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        separator = '&';
      }
    }
    try {
      return URI.create(url.toString());
    } catch (IllegalArgumentException e) {
      throw new ConfigurationException("Invalid request path: " + path);
    }
  }

  /** Encodes one path segment (an order id, a link id). */
  public static String segment(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  /** Encodes a path remainder segment by segment, keeping {@code /} as the separator. */
  public static String remainder(String value) {
    String[] parts = value.split("/", -1);
    StringBuilder encoded = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      if (i > 0) {
        encoded.append('/');
      }
      encoded.append(segment(parts[i]));
    }
    return encoded.toString();
  }

  private void pause(long delayMillis) {
    try {
      sleeper.sleep(delayMillis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ConnectionException("Interrupted while waiting to retry", e);
    }
  }

  private void log(LogLevel level, String message, Map<String, Object> fields) {
    if (logger != null) {
      logger.log(level, message, fields);
    }
  }

  private static Map<String, Object> fields(
      String method,
      String path,
      Integer status,
      long durationMs,
      String requestId,
      int attempt,
      String code,
      Long delayMs) {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("method", method);
    fields.put("path", API_PREFIX + path);
    if (status != null) {
      fields.put("status", status);
    }
    fields.put("durationMs", durationMs);
    if (requestId != null) {
      fields.put("requestId", requestId);
    }
    fields.put("attempt", attempt);
    if (code != null) {
      fields.put("code", code);
    }
    if (delayMs != null) {
      fields.put("delayMs", delayMs);
    }
    return fields;
  }

  private static long elapsedMillis(long startedNanos) {
    return (System.nanoTime() - startedNanos) / 1_000_000L;
  }

  private static String describe(IOException e) {
    String name = e.getClass().getSimpleName();
    String detail = e.getMessage();
    return detail == null || detail.isEmpty() ? name : name + ": " + detail;
  }
}
