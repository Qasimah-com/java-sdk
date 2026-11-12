package com.qasimha.sdk.model;

import java.math.BigDecimal;
import java.time.Instant;

/** One settlement batch paid to the merchant, net of fees. */
public final class Settlement extends ApiObject {

  private String id;
  private String providerSettlementId;
  private BigDecimal amount;
  private String currencyCode;
  private Instant settledAt;
  private String status;
  private Integer orderCount;

  private Settlement() {}

  public String getId() {
    return id;
  }

  public String getProviderSettlementId() {
    return providerSettlementId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public Instant getSettledAt() {
    return settledAt;
  }

  public String getStatus() {
    return status;
  }

  public Integer getOrderCount() {
    return orderCount;
  }
}
