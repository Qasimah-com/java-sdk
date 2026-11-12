package com.qasimha.sdk.model;

import java.math.BigDecimal;

/**
 * The result of {@code settlements().overview()}. The named figures are typed; everything else the
 * API reports ({@code lastSettlement}, the not-paid-yet balance, ...) is in {@link #getRaw()}.
 */
public final class SettlementOverview extends ApiObject {

  private BigDecimal totalSettled;
  private Long count;
  private String settlementCycle;
  private String settlementModel;
  private BigDecimal minimumSettlementAmount;
  private BigDecimal remainingForNextSettlement;

  private SettlementOverview() {}

  public BigDecimal getTotalSettled() {
    return totalSettled;
  }

  public Long getCount() {
    return count;
  }

  public String getSettlementCycle() {
    return settlementCycle;
  }

  public String getSettlementModel() {
    return settlementModel;
  }

  public BigDecimal getMinimumSettlementAmount() {
    return minimumSettlementAmount;
  }

  public BigDecimal getRemainingForNextSettlement() {
    return remainingForNextSettlement;
  }
}
