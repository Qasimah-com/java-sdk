package com.qasimha.sdk.model;

import java.math.BigDecimal;

/** The body of {@code POST /merchant/orders/{id}/refund}. */
public final class RefundRequest {

  private BigDecimal amount;
  private String reason;

  private RefundRequest(BigDecimal amount, String reason) {
    this.amount = amount;
    this.reason = reason;
  }

  /** A refund of {@code amount} with no reason. */
  public static RefundRequest of(BigDecimal amount) {
    return new RefundRequest(amount, null);
  }

  /** A refund of {@code amount} with a reason shown in the back office. */
  public static RefundRequest of(BigDecimal amount, String reason) {
    return new RefundRequest(amount, reason);
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getReason() {
    return reason;
  }
}
