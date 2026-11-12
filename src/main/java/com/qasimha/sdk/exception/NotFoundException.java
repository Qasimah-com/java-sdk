package com.qasimha.sdk.exception;

import com.qasimha.sdk.model.FieldError;
import java.util.List;
import java.util.Map;

/** A 404: the resource does not exist or belongs to another merchant. */
public class NotFoundException extends QasimhaException {

  private static final long serialVersionUID = 1L;

  public NotFoundException(
      String message,
      String code,
      String requestId,
      List<FieldError> errors,
      Map<String, Object> rawEnvelope) {
    super(message, code, 404, requestId, errors, rawEnvelope, null);
  }
}
