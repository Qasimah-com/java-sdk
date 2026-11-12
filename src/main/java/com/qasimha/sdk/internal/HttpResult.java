package com.qasimha.sdk.internal;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpHeaders;
import java.util.Optional;

/** One HTTP response as seen by the resources, with the envelope already decoded. */
public final class HttpResult {

  private final int status;
  private final HttpHeaders headers;
  private final byte[] body;
  private final int attempt;
  private final Envelope envelope;

  public HttpResult(int status, HttpHeaders headers, byte[] body, int attempt) {
    this.status = status;
    this.headers = headers;
    this.body = body == null ? new byte[0] : body;
    this.attempt = attempt;
    JsonNode root = Json.readTree(this.body);
    this.envelope = Envelope.parse(root);
  }

  public int getStatus() {
    return status;
  }

  public boolean isSuccess() {
    return status >= 200 && status < 300;
  }

  public HttpHeaders getHeaders() {
    return headers;
  }

  public Optional<String> header(String name) {
    return headers == null ? Optional.empty() : headers.firstValue(name);
  }

  public byte[] getBody() {
    return body;
  }

  /** The 1-based attempt that produced this response. */
  public int getAttempt() {
    return attempt;
  }

  /** The decoded envelope, or null when the body was empty or not JSON. */
  public Envelope getEnvelope() {
    return envelope;
  }

  /** The envelope {@code code}, or null. */
  public String getCode() {
    return envelope == null ? null : envelope.getCode();
  }

  /** The envelope {@code uuid}, else the {@code X-Request-Id} header, else null. */
  public String getRequestId() {
    if (envelope != null && envelope.getUuid() != null && !envelope.getUuid().isEmpty()) {
      return envelope.getUuid();
    }
    return header("X-Request-Id").orElse(null);
  }

  /** Whether the response carried {@code Idempotent-Replay: true}. */
  public boolean isReplayed() {
    return header("Idempotent-Replay").map(v -> v.trim().equalsIgnoreCase("true")).orElse(false);
  }
}
