package com.qasimha.sdk.exception;

import com.qasimha.sdk.model.FieldError;
import java.util.List;
import java.util.Map;

/**
 * Any other unsuccessful response: an unmapped 4xx, a 3xx (the SDK never follows redirects), or a
 * 2xx whose envelope says {@code success: false}.
 */
public class ApiException extends QasimhaException {

  private static final long serialVersionUID = 1L;

  public ApiException(
      String message,
      String code,
      Integer httpStatus,
      String requestId,
      List<FieldError> errors,
      Map<String, Object> rawEnvelope) {
    super(message, code, httpStatus, requestId, errors, rawEnvelope, null);
  }
}
