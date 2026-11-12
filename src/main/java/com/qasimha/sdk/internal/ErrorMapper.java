package com.qasimha.sdk.internal;

import com.qasimha.sdk.exception.ApiException;
import com.qasimha.sdk.exception.AuthenticationException;
import com.qasimha.sdk.exception.ConflictException;
import com.qasimha.sdk.exception.DuplicateOrderReferenceException;
import com.qasimha.sdk.exception.NotFoundException;
import com.qasimha.sdk.exception.OrderDeclinedException;
import com.qasimha.sdk.exception.PermissionException;
import com.qasimha.sdk.exception.QasimhaException;
import com.qasimha.sdk.exception.RateLimitException;
import com.qasimha.sdk.exception.ServerException;
import com.qasimha.sdk.exception.ValidationException;
import com.qasimha.sdk.model.FieldError;
import java.time.Clock;
import java.util.List;
import java.util.Map;

/** Chooses the exception subtype for an unsuccessful response: HTTP status first, then code. */
public final class ErrorMapper {

  public static final String DUPLICATE_ORDER_REFERENCE = "DUPLICATE_ORDER_REFERENCE";
  public static final String ORDER_DECLINED = "ORDER_DECLINED";
  public static final String REQUEST_IN_PROGRESS = "REQUEST_IN_PROGRESS";

  private ErrorMapper() {}

  public static QasimhaException map(HttpResult result, Clock clock) {
    int status = result.getStatus();
    Envelope envelope = result.getEnvelope();
    String code = envelope == null ? null : envelope.getCode();
    String requestId = result.getRequestId();
    List<FieldError> errors = envelope == null ? null : envelope.getErrors();
    Map<String, Object> raw = envelope == null ? null : envelope.getRaw();
    String message = message(status, envelope);

    switch (status) {
      case 400:
        return new ValidationException(message, code, status, requestId, errors, raw);
      case 401:
        return new AuthenticationException(message, code, requestId, raw);
      case 403:
        return new PermissionException(message, code, requestId, errors, raw);
      case 404:
        return new NotFoundException(message, code, requestId, errors, raw);
      case 409:
        if (DUPLICATE_ORDER_REFERENCE.equals(code)) {
          return new DuplicateOrderReferenceException(
              message,
              code,
              requestId,
              errors,
              raw,
              envelope.dataText("installmentId"),
              envelope.dataText("paymentUrl"));
        }
        return new ConflictException(message, code, requestId, errors, raw);
      case 422:
        if (ORDER_DECLINED.equals(code)) {
          return new OrderDeclinedException(
              message, code, status, requestId, errors, raw, envelope.dataText("installmentId"));
        }
        return new ApiException(message, code, status, requestId, errors, raw);
      case 429:
        return new RateLimitException(
            message,
            code,
            requestId,
            errors,
            raw,
            Backoff.parseRetryAfterSeconds(result.header("Retry-After").orElse(null), clock));
      default:
        if (status >= 500) {
          return new ServerException(message, code, status, requestId, errors, raw);
        }
        return new ApiException(message, code, status, requestId, errors, raw);
    }
  }

  private static String message(int status, Envelope envelope) {
    if (envelope != null && envelope.getMessage() != null && !envelope.getMessage().isEmpty()) {
      return envelope.getMessage();
    }
    if (envelope != null && envelope.getCode() != null) {
      return envelope.getCode() + " (HTTP " + status + ")";
    }
    switch (status) {
      case 401:
        return "The API key is missing, invalid or revoked (HTTP 401)";
      case 403:
        return "The API key lacks the scope this call requires (HTTP 403)";
      case 404:
        return "Not found (HTTP 404)";
      case 429:
        return "Rate limited (HTTP 429)";
      default:
        if (status >= 300 && status < 400) {
          return "Unexpected redirect (HTTP " + status + ")";
        }
        return "Unexpected response (HTTP " + status + ")";
    }
  }
}
