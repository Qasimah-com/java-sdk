package com.qasimha.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The result of {@code checkout().createQrSession}: render {@link #getQrCodeUrl()} as a QR code.
 */
public final class QrSession extends ApiObject {

  @JsonProperty("installmentId")
  private String orderId;

  private String qrCodeUrl;
  private String providerSessionId;
  private OrderStatus status;

  private QrSession() {}

  /** The Qasimha order id ({@code installmentId}). */
  public String getOrderId() {
    return orderId;
  }

  /** The URL the customer scans. */
  public String getQrCodeUrl() {
    return qrCodeUrl;
  }

  public String getProviderSessionId() {
    return providerSessionId;
  }

  public OrderStatus getStatus() {
    return status;
  }
}
