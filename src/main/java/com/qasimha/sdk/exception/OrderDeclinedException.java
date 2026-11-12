package com.qasimha.sdk.exception;

import com.qasimha.sdk.model.FieldError;
import java.util.List;
import java.util.Map;

/**
 * A {@code 422 ORDER_DECLINED}: the order was created but not approved, so there is nothing to pay.
 * {@link #getOrderId()} is the declined order; read it with {@code orders().get(id)} for the
 * decision.
 */
public class OrderDeclinedException extends QasimhaException {

  private static final long serialVersionUID = 1L;

  private final String orderId;

  public OrderDeclinedException(
      String message,
      String code,
      Integer httpStatus,
      String requestId,
      List<FieldError> errors,
      Map<String, Object> rawEnvelope,
      String orderId) {
    super(message, code, httpStatus, requestId, errors, rawEnvelope, null);
    this.orderId = orderId;
  }

  /** The Qasimha id of the declined order ({@code data.installmentId}); may be null. */
  public String getOrderId() {
    return orderId;
  }
}
