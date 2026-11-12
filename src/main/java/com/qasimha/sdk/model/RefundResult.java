package com.qasimha.sdk.model;

import java.math.BigDecimal;

/** The result of {@code orders().refund(id, ...)}. */
public final class RefundResult extends ApiObject {

  private String id;
  private OrderStatus status;
  private BigDecimal refundedAmount;

  private RefundResult() {}

  public String getId() {
    return id;
  }

  public OrderStatus getStatus() {
    return status;
  }

  /** The total refunded on the order so far. */
  public BigDecimal getRefundedAmount() {
    return refundedAmount;
  }
}
