package com.qasimha.sdk.exception;

import com.qasimha.sdk.model.FieldError;
import java.util.List;
import java.util.Map;

/**
 * A 409 other than a duplicate order reference: {@code MERCHANT_SUSPENDED}, {@code INVALID_STATE},
 * {@code NOT_OPEN}, or {@code REQUEST_IN_PROGRESS} once the retries are exhausted.
 */
public class ConflictException extends QasimhaException {

  private static final long serialVersionUID = 1L;

  public ConflictException(
      String message,
      String code,
      String requestId,
      List<FieldError> errors,
      Map<String, Object> rawEnvelope) {
    super(message, code, 409, requestId, errors, rawEnvelope, null);
  }
}
