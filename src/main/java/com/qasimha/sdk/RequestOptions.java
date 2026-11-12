package com.qasimha.sdk;

import com.qasimha.sdk.exception.ConfigurationException;

/**
 * Per-call options. By default every {@code POST} carries a fresh UUID v4 {@code Idempotency-Key};
 * a caller may supply its own key or disable idempotency, which also disables retries for that
 * call.
 */
public final class RequestOptions {

  private static final RequestOptions DEFAULT = new RequestOptions(null, false);

  private final String idempotencyKey;
  private final boolean idempotencyDisabled;

  private RequestOptions(String idempotencyKey, boolean idempotencyDisabled) {
    this.idempotencyKey = idempotencyKey;
    this.idempotencyDisabled = idempotencyDisabled;
  }

  /** The default options: a generated UUID v4 idempotency key. */
  public static RequestOptions defaults() {
    return DEFAULT;
  }

  /** Uses the caller's own idempotency key for this call. */
  public static RequestOptions withIdempotencyKey(String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
      throw new ConfigurationException("idempotencyKey must not be blank");
    }
    if (idempotencyKey.length() > 255 || !idempotencyKey.chars().allMatch(c -> c > 32 && c < 127)) {
      throw new ConfigurationException(
          "idempotencyKey must be printable ASCII of at most 255 characters");
    }
    return new RequestOptions(idempotencyKey, false);
  }

  /** Sends no {@code Idempotency-Key}; the call is then never retried. */
  public static RequestOptions withoutIdempotency() {
    return new RequestOptions(null, true);
  }

  /** The caller-chosen idempotency key, or {@code null} to generate one. */
  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  /** Whether the {@code Idempotency-Key} header is suppressed for this call. */
  public boolean isIdempotencyDisabled() {
    return idempotencyDisabled;
  }
}
