package com.qasimha.sdk.model;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import java.util.Map;

/** Base of every model read from an API response. */
public abstract class ApiObject {

  @JsonIgnore
  @JacksonInject(value = ResponseMeta.INJECT_ID, useInput = OptBoolean.FALSE)
  private ResponseMeta sdkResponseMeta = ResponseMeta.NONE;

  /** The untyped decoded JSON this model was read from; every field, typed or not. */
  public Map<String, Object> getRaw() {
    return sdkResponseMeta.getRaw();
  }

  /** The request id (envelope {@code uuid}, else the {@code X-Request-Id} header). */
  public String getRequestId() {
    return sdkResponseMeta.getRequestId();
  }

  /** Whether the response carried {@code Idempotent-Replay: true}. */
  public boolean isReplayed() {
    return sdkResponseMeta.isReplayed();
  }
}
