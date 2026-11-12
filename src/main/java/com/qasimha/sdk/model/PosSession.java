package com.qasimha.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The result of {@code checkout().createPosSession}: the hosted page at {@link #getCheckoutUrl()}.
 */
public final class PosSession extends ApiObject {

  @JsonProperty("installmentId")
  private String orderId;

  private String checkoutUrl;
  private String providerSessionId;
  private OrderStatus status;

  private PosSession() {}

  /** The Qasimha order id ({@code installmentId}). */
  public String getOrderId() {
    return orderId;
  }

  /** The hosted checkout page, {@code {checkout host}/pay/{orderId}}. */
  public String getCheckoutUrl() {
    return checkoutUrl;
  }

  public String getProviderSessionId() {
    return providerSessionId;
  }

  /** {@code AwaitingCheckout} on creation. */
  public OrderStatus getStatus() {
    return status;
  }
}
