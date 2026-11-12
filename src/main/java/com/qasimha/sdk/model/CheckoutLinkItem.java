package com.qasimha.sdk.model;

import java.math.BigDecimal;

/** A line on a checkout link; {@code total} is only present on the created link. */
public final class CheckoutLinkItem {

  private String name;
  private String sku;
  private Integer quantity;
  private BigDecimal unitPrice;
  private BigDecimal total;
  private String imageUrl;

  private CheckoutLinkItem() {}

  private CheckoutLinkItem(Builder builder) {
    this.name = builder.name;
    this.sku = builder.sku;
    this.quantity = builder.quantity;
    this.unitPrice = builder.unitPrice;
    this.imageUrl = builder.imageUrl;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getName() {
    return name;
  }

  public String getSku() {
    return sku;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  /** {@code quantity × unitPrice} as computed by the API on creation; null on a request. */
  public BigDecimal getTotal() {
    return total;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  /** Builds a {@link CheckoutLinkItem}. */
  public static final class Builder {
    private String name;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private String imageUrl;

    private Builder() {}

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder sku(String sku) {
      this.sku = sku;
      return this;
    }

    public Builder quantity(int quantity) {
      this.quantity = quantity;
      return this;
    }

    public Builder unitPrice(BigDecimal unitPrice) {
      this.unitPrice = unitPrice;
      return this;
    }

    public Builder imageUrl(String imageUrl) {
      this.imageUrl = imageUrl;
      return this;
    }

    public CheckoutLinkItem build() {
      return new CheckoutLinkItem(this);
    }
  }
}
