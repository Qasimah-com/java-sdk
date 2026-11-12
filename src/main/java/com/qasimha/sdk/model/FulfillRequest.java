package com.qasimha.sdk.model;

import java.math.BigDecimal;

/**
 * The body of {@code POST /merchant/orders/{id}/fulfill}. Leave {@code shippedAmount} unset to
 * fulfil everything still outstanding; a positive figure records a partial shipment.
 */
public final class FulfillRequest {

  private BigDecimal shippedAmount;
  private String shippingCompany;
  private String trackingNumber;
  private String trackingUrl;

  private FulfillRequest(Builder builder) {
    this.shippedAmount = builder.shippedAmount;
    this.shippingCompany = builder.shippingCompany;
    this.trackingNumber = builder.trackingNumber;
    this.trackingUrl = builder.trackingUrl;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Fulfils the whole outstanding amount with no shipment details. */
  public static FulfillRequest all() {
    return builder().build();
  }

  public BigDecimal getShippedAmount() {
    return shippedAmount;
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

  /** Builds a {@link FulfillRequest}. */
  public static final class Builder {
    private BigDecimal shippedAmount;
    private String shippingCompany;
    private String trackingNumber;
    private String trackingUrl;

    private Builder() {}

    public Builder shippedAmount(BigDecimal shippedAmount) {
      this.shippedAmount = shippedAmount;
      return this;
    }

    public Builder shippingCompany(String shippingCompany) {
      this.shippingCompany = shippingCompany;
      return this;
    }

    public Builder trackingNumber(String trackingNumber) {
      this.trackingNumber = trackingNumber;
      return this;
    }

    public Builder trackingUrl(String trackingUrl) {
      this.trackingUrl = trackingUrl;
      return this;
    }

    public FulfillRequest build() {
      return new FulfillRequest(this);
    }
  }
}
