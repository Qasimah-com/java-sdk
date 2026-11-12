package com.qasimha.sdk.exception;

import com.qasimha.sdk.model.FieldError;
import java.util.List;
import java.util.Map;

/**
 * A {@code 409 DUPLICATE_ORDER_REFERENCE}: the {@code orderReferenceId} is already in use. When the
 * existing order is yours, {@link #getExistingOrderId()} and {@link #getExistingPaymentUrl()} point
 * at the checkout already open.
 */
public class DuplicateOrderReferenceException extends ConflictException {

  private static final long serialVersionUID = 1L;

  private final String existingOrderId;
  private final String existingPaymentUrl;

  public DuplicateOrderReferenceException(
      String message,
      String code,
      String requestId,
      List<FieldError> errors,
      Map<String, Object> rawEnvelope,
      String existingOrderId,
      String existingPaymentUrl) {
    super(message, code, requestId, errors, rawEnvelope);
    this.existingOrderId = existingOrderId;
    this.existingPaymentUrl = existingPaymentUrl;
  }

  /**
   * The Qasimha id of the order that already carries this reference ({@code data.installmentId}).
   */
  public String getExistingOrderId() {
    return existingOrderId;
  }

  /** The payment URL of the existing order ({@code data.paymentUrl}); may be null. */
  public String getExistingPaymentUrl() {
    return existingPaymentUrl;
  }
}
