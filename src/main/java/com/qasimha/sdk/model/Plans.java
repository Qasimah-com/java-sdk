package com.qasimha.sdk.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * The result of {@code checkout().getPlans(amount)}: the plans the merchant's policy offers for the
 * amount. {@link #getPlans()} is empty, never null, when the amount is outside {@code [minAmount,
 * maxAmount]} or the account has no active policy.
 */
public final class Plans extends ApiObject {

  private String merchantName;
  private String currency;
  private BigDecimal minAmount;
  private BigDecimal maxAmount;
  private List<Plan> plans;

  private Plans() {}

  /** The merchant's display name, or null. */
  public String getMerchantName() {
    return merchantName;
  }

  /** Always {@code SAR}. */
  public String getCurrency() {
    return currency;
  }

  /** The smallest amount the policy finances. */
  public BigDecimal getMinAmount() {
    return minAmount;
  }

  /** The largest amount the policy finances. */
  public BigDecimal getMaxAmount() {
    return maxAmount;
  }

  /** The quoted plans, one per tenor; empty when nothing is offered for the amount. */
  public List<Plan> getPlans() {
    return plans == null ? Collections.emptyList() : Collections.unmodifiableList(plans);
  }

  /** Whether at least one plan is offered for the amount. */
  public boolean isOffered() {
    return plans != null && !plans.isEmpty();
  }
}
