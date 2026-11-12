package com.qasimha.sdk.model;

/** The result of {@code orders().cancel(id)}. */
public final class CancelResult extends ApiObject {

  private String id;
  private OrderStatus status;

  private CancelResult() {}

  public String getId() {
    return id;
  }

  public OrderStatus getStatus() {
    return status;
  }
}
