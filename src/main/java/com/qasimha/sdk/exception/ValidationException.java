package com.qasimha.sdk.exception;

import com.qasimha.sdk.model.FieldError;
import java.util.List;
import java.util.Map;

/**
 * A 400 from the API ({@code VALIDATION_ERROR}, {@code INVALID_AMOUNT}, {@code PHONE_REQUIRED},
 * {@code INVALID_STATUS}, {@code AMOUNT_EXCEEDED}, ...) or a client-side check that failed before
 * the request was sent; {@link #getErrors()} names the fields.
 */
public class ValidationException extends QasimhaException {

  private static final long serialVersionUID = 1L;

  public ValidationException(
      String message,
      String code,
      Integer httpStatus,
      String requestId,
      List<FieldError> errors,
      Map<String, Object> rawEnvelope) {
    super(message, code, httpStatus, requestId, errors, rawEnvelope, null);
  }

  /** A client-side validation failure: no request was sent, {@code httpStatus} is null. */
  public static ValidationException clientSide(List<FieldError> errors) {
    StringBuilder message = new StringBuilder("Request failed client-side validation");
    if (errors != null && !errors.isEmpty()) {
      message.append(": ").append(errors.get(0));
      if (errors.size() > 1) {
        message.append(" (+").append(errors.size() - 1).append(" more)");
      }
    }
    return new ValidationException(message.toString(), null, null, null, errors, null);
  }

  /** Whether this failure was raised locally, before any request was made. */
  public boolean isClientSide() {
    return getHttpStatus() == null;
  }
}
