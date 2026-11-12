package com.qasimha.sdk.exception;

import com.qasimha.sdk.model.FieldError;
import java.util.List;
import java.util.Map;

/** A 5xx. A 500 is never retried because a write may already have committed. */
public class ServerException extends ApiException {

  private static final long serialVersionUID = 1L;

  public ServerException(
      String message,
      String code,
      Integer httpStatus,
      String requestId,
      List<FieldError> errors,
      Map<String, Object> rawEnvelope) {
    super(message, code, httpStatus, requestId, errors, rawEnvelope);
  }
}
