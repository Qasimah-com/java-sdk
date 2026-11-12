package com.qasimha.sdk.exception;

import com.qasimha.sdk.model.FieldError;
import java.util.List;
import java.util.Map;

/** A 429 once the retries are exhausted; carries {@code Retry-After} in seconds when present. */
public class RateLimitException extends QasimhaException {

  private static final long serialVersionUID = 1L;

  private final Long retryAfterSeconds;

  public RateLimitException(
      String message,
      String code,
      String requestId,
      List<FieldError> errors,
      Map<String, Object> rawEnvelope,
      Long retryAfterSeconds) {
    super(message, code, 429, requestId, errors, rawEnvelope, null);
    this.retryAfterSeconds = retryAfterSeconds;
  }

  /** Seconds to wait as announced by the {@code Retry-After} header, or null when absent. */
  public Long getRetryAfterSeconds() {
    return retryAfterSeconds;
  }
}
