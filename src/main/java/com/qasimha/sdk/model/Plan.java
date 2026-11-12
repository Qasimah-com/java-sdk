package com.qasimha.sdk.model;

import java.math.BigDecimal;

/**
 * One instalment plan quoted for an amount: the lowest price the merchant's policy lists for that
 * tenor, with no customer data involved. The checkout quotes the customer's own terms.
 */
public final class Plan {

  private Integer instalments;
  private String frequency;
  private BigDecimal instalmentAmount;
  private BigDecimal firstInstalmentAmount;
  private BigDecimal lastInstalmentAmount;
  private BigDecimal feesIncludingVat;
  private BigDecimal totalPayable;
  private Boolean shariaCompliant;

  private Plan() {}

  /** The number of instalments. */
  public Integer getInstalments() {
    return instalments;
  }

  /** How often an instalment falls due, for example {@code monthly}. */
  public String getFrequency() {
    return frequency;
  }

  /** The regular instalment amount. */
  public BigDecimal getInstalmentAmount() {
    return instalmentAmount;
  }

  /** The first instalment, paid at checkout. */
  public BigDecimal getFirstInstalmentAmount() {
    return firstInstalmentAmount;
  }

  /** The last instalment, which carries any rounding residual. */
  public BigDecimal getLastInstalmentAmount() {
    return lastInstalmentAmount;
  }

  /** The fees over the amount, VAT included. */
  public BigDecimal getFeesIncludingVat() {
    return feesIncludingVat;
  }

  /** The amount plus the fees. */
  public BigDecimal getTotalPayable() {
    return totalPayable;
  }

  /** Whether the plan is Sharia-compliant. */
  public Boolean getShariaCompliant() {
    return shariaCompliant;
  }
}
