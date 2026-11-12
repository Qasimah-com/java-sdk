package com.qasimha.sdk.model;

import java.time.Instant;

/** One entry of an order's status history. */
public final class StatusChange {

  private OrderStatus fromStatus;
  private OrderStatus toStatus;
  private String trigger;
  private String metadata;
  private Instant createdAt;

  private StatusChange() {}

  public OrderStatus getFromStatus() {
    return fromStatus;
  }

  public OrderStatus getToStatus() {
    return toStatus;
  }

  public String getTrigger() {
    return trigger;
  }

  public String getMetadata() {
    return metadata;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
