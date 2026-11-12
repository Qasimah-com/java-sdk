package com.qasimha.sdk.model;

import java.math.BigDecimal;
import java.time.Instant;

/** One refund (reversal) recorded against an order. */
public final class Reversal {

  private String id;
  private BigDecimal amount;
  private String currencyCode;
  private String comment;
  private String fulfillmentId;
  private String providerRefundId;
  private Instant createdAt;

  private Reversal() {}

  public String getId() {
    return id;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public String getComment() {
    return comment;
  }

  public String getFulfillmentId() {
    return fulfillmentId;
  }

  public String getProviderRefundId() {
    return providerRefundId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
