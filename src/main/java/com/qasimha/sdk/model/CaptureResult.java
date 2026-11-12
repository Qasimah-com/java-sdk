package com.qasimha.sdk.model;

import java.time.Instant;

/** The result of {@code orders().capture(id)}. */
public final class CaptureResult extends ApiObject {

  private String id;
  private OrderStatus status;
  private Instant confirmedAt;

  private CaptureResult() {}

  public String getId() {
    return id;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public Instant getConfirmedAt() {
    return confirmedAt;
  }
}
