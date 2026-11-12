package com.qasimha.sdk.model;

import java.util.Collections;
import java.util.Map;

/**
 * What the SDK knows about the response a model came from: the untyped decoded {@code data}, the
 * request id and whether the response was an idempotent replay.
 */
public final class ResponseMeta {

  /** The injectable id under which the SDK's JSON mapper supplies the metadata. */
  public static final String INJECT_ID = "com.qasimha.sdk.responseMeta";

  /** Metadata for a model that was not read from an API response. */
  public static final ResponseMeta NONE = new ResponseMeta(null, null, false);

  private final Map<String, Object> raw;
  private final String requestId;
  private final boolean replayed;

  public ResponseMeta(Map<String, Object> raw, String requestId, boolean replayed) {
    this.raw = raw == null ? Collections.emptyMap() : Collections.unmodifiableMap(raw);
    this.requestId = requestId;
    this.replayed = replayed;
  }

  /** The decoded JSON the model was read from. */
  public Map<String, Object> getRaw() {
    return raw;
  }

  /** The request id (envelope {@code uuid}, else the {@code X-Request-Id} header). */
  public String getRequestId() {
    return requestId;
  }

  /** Whether the response carried {@code Idempotent-Replay: true}. */
  public boolean isReplayed() {
    return replayed;
  }
}
