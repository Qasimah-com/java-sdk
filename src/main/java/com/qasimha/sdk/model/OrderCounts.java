package com.qasimha.sdk.model;

/** The result of {@code orders().counts()}: per-tab totals for a dashboard. */
public final class OrderCounts extends ApiObject {

  private long all;
  private long approved;
  private long authorised;
  private long captured;
  private long refunded;
  private long cancelled;

  private OrderCounts() {}

  public long getAll() {
    return all;
  }

  /** {@code New} and {@code Approved} orders. */
  public long getApproved() {
    return approved;
  }

  /** {@code Confirmed} orders. */
  public long getAuthorised() {
    return authorised;
  }

  /** {@code FullyFulfilled} and {@code PartiallyFulfilled} orders. */
  public long getCaptured() {
    return captured;
  }

  /** {@code FullyReversed} and {@code PartiallyReversed} orders. */
  public long getRefunded() {
    return refunded;
  }

  /** {@code Voided} orders. */
  public long getCancelled() {
    return cancelled;
  }
}
