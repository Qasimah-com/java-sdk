package com.qasimha.sdk.model;

import java.math.BigDecimal;
import java.time.Instant;

/** One scheduled instalment of an order. */
public final class Installment {

  private Integer sequence;
  private Instant dueDate;
  private BigDecimal amount;
  private BigDecimal principal;
  private BigDecimal profit;
  private InstallmentItemStatus status;
  private BigDecimal paidAmount;
  private Instant paidAt;
  private String providerRef;

  private Installment() {}

  public Integer getSequence() {
    return sequence;
  }

  public Instant getDueDate() {
    return dueDate;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getPrincipal() {
    return principal;
  }

  public BigDecimal getProfit() {
    return profit;
  }

  public InstallmentItemStatus getStatus() {
    return status;
  }

  public BigDecimal getPaidAmount() {
    return paidAmount;
  }

  public Instant getPaidAt() {
    return paidAt;
  }

  public String getProviderRef() {
    return providerRef;
  }
}
