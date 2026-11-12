package com.qasimha.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The result of {@code checkout().createSession}: redirect the shopper to {@link #getPaymentUrl()}.
 */
public final class Session extends ApiObject {

  @JsonProperty("installmentId")
  private String orderId;

  private String paymentUrl;
  private String providerSessionId;
  private OrderStatus status;

  private Session() {}

  /** The Qasimha order id ({@code installmentId}); use it for every later order call. */
  public String getOrderId() {
    return orderId;
  }

  /** The hosted checkout URL; always present on a created session. */
  public String getPaymentUrl() {
    return paymentUrl;
  }

  public String getProviderSessionId() {
    return providerSessionId;
  }

  public OrderStatus getStatus() {
    return status;
  }
}
