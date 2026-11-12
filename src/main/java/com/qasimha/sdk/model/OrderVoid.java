package com.qasimha.sdk.model;

import java.math.BigDecimal;
import java.time.Instant;

/** One void (cancellation) recorded against an order. */
public final class OrderVoid {

  private String id;
  private BigDecimal amount;
  private String currencyCode;
  private String reason;
  private String providerCancelId;
  private Instant createdAt;

  private OrderVoid() {}

  public String getId() {
    return id;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public String getReason() {
    return reason;
  }

  public String getProviderCancelId() {
    return providerCancelId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
