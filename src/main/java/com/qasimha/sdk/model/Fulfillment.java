package com.qasimha.sdk.model;

import java.math.BigDecimal;
import java.time.Instant;

/** One fulfilment (shipment) recorded against an order. */
public final class Fulfillment {

  private String id;
  private BigDecimal amount;
  private String currencyCode;
  private String shippingCompany;
  private String trackingNumber;
  private String trackingUrl;
  private String providerCaptureId;
  private Instant createdAt;

  private Fulfillment() {}

  public String getId() {
    return id;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public String getShippingCompany() {
    return shippingCompany;
  }

  public String getTrackingNumber() {
    return trackingNumber;
  }

  public String getTrackingUrl() {
    return trackingUrl;
  }

  public String getProviderCaptureId() {
    return providerCaptureId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
