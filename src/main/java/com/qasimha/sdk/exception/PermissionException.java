package com.qasimha.sdk.exception;

import com.qasimha.sdk.model.FieldError;
import java.util.List;
import java.util.Map;

/** A 403: the key is valid but lacks the scope the endpoint needs. */
public class PermissionException extends QasimhaException {

  private static final long serialVersionUID = 1L;

  public PermissionException(
      String message,
      String code,
      String requestId,
      List<FieldError> errors,
      Map<String, Object> rawEnvelope) {
    super(message, code, 403, requestId, errors, rawEnvelope, null);
  }
}
