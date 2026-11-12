package com.qasimha.sdk.exception;

import com.qasimha.sdk.model.FieldError;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Base of every exception the SDK raises. Carries the envelope {@code code}, the HTTP status
 * ({@code null} for transport failures), the request id (envelope {@code uuid}), the field errors
 * and the raw decoded envelope. Never contains the API key.
 */
public class QasimhaException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String code;
  private final Integer httpStatus;
  private final String requestId;
  private final transient List<FieldError> errors;
  private final transient Map<String, Object> rawEnvelope;

  public QasimhaException(String message) {
    this(message, null, null, null, null, null, null);
  }

  public QasimhaException(String message, Throwable cause) {
    this(message, null, null, null, null, null, cause);
  }

  public QasimhaException(
      String message,
      String code,
      Integer httpStatus,
      String requestId,
      List<FieldError> errors,
      Map<String, Object> rawEnvelope,
      Throwable cause) {
    super(message, cause);
    this.code = code;
    this.httpStatus = httpStatus;
    this.requestId = requestId;
    this.errors = errors == null ? Collections.emptyList() : Collections.unmodifiableList(errors);
    this.rawEnvelope =
        rawEnvelope == null ? Collections.emptyMap() : Collections.unmodifiableMap(rawEnvelope);
  }

  /** The envelope {@code code} ({@code VALIDATION_ERROR}, {@code ORDER_DECLINED}, ...) or null. */
  public String getCode() {
    return code;
  }

  /** The HTTP status, or {@code null} when no response was received. */
  public Integer getHttpStatus() {
    return httpStatus;
  }

  /** The request id from the envelope {@code uuid} or the {@code X-Request-Id} header. */
  public String getRequestId() {
    return requestId;
  }

  /** Field-level errors; empty when none were reported. */
  public List<FieldError> getErrors() {
    return errors;
  }

  /** The decoded response envelope; empty when the response had no JSON body. */
  public Map<String, Object> getRawEnvelope() {
    return rawEnvelope;
  }

  @Override
  public String toString() {
    StringBuilder sb =
        new StringBuilder(getClass().getSimpleName()).append(": ").append(getMessage());
    if (code != null) {
      sb.append(" [code=").append(code).append(']');
    }
    if (httpStatus != null) {
      sb.append(" [status=").append(httpStatus).append(']');
    }
    if (requestId != null) {
      sb.append(" [requestId=").append(requestId).append(']');
    }
    if (!errors.isEmpty()) {
      sb.append(" errors=").append(errors);
    }
    return sb.toString();
  }
}
