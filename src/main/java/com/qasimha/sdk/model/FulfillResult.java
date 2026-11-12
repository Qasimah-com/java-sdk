package com.qasimha.sdk.model;

import java.math.BigDecimal;

/** The result of {@code orders().fulfill(id, ...)}. */
public final class FulfillResult extends ApiObject {

  private String id;
  private OrderStatus status;
  private BigDecimal fulfilledAmount;

  private FulfillResult() {}

  public String getId() {
    return id;
  }

  public OrderStatus getStatus() {
    return status;
  }

  /** The total fulfilled on the order so far. */
  public BigDecimal getFulfilledAmount() {
    return fulfilledAmount;
  }
}
