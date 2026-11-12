package com.qasimha.sdk.model;

import java.math.BigDecimal;

/**
 * One basket line, both as sent when creating a session and as read back on an order. {@code
 * totalAmount} must equal {@code quantity × unitPrice − discountAmount}; the builder computes it
 * when it is left unset.
 */
public final class Item {

  /** The {@code type} of a physical good. */
  public static final String TYPE_PHYSICAL = "physical";

  /** The {@code type} of a digital good or service. */
  public static final String TYPE_DIGITAL = "digital";

  private String referenceId;
  private String name;
  private String type;
  private String sku;
  private Integer quantity;
  private BigDecimal unitPrice;
  private BigDecimal totalAmount;
  private BigDecimal taxAmount;
  private BigDecimal discountAmount;
  private String imageUrl;

  private Item() {}

  private Item(Builder builder) {
    this.referenceId = builder.referenceId;
    this.name = builder.name;
    this.type = builder.type;
    this.sku = builder.sku;
    this.quantity = builder.quantity;
    this.unitPrice = builder.unitPrice;
    this.totalAmount = builder.totalAmount;
    this.taxAmount = builder.taxAmount;
    this.discountAmount = builder.discountAmount;
    this.imageUrl = builder.imageUrl;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getReferenceId() {
    return referenceId;
  }

  public String getName() {
    return name;
  }

  /** {@link #TYPE_PHYSICAL} or {@link #TYPE_DIGITAL}. */
  public String getType() {
    return type;
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

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public BigDecimal getTaxAmount() {
    return taxAmount;
  }

  public BigDecimal getDiscountAmount() {
    return discountAmount;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  /** Builds an {@link Item}. */
  public static final class Builder {
    private String referenceId;
    private String name;
    private String type;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private String imageUrl;

    private Builder() {}

    public Builder referenceId(String referenceId) {
      this.referenceId = referenceId;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
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

    public Builder totalAmount(BigDecimal totalAmount) {
      this.totalAmount = totalAmount;
      return this;
    }

    public Builder taxAmount(BigDecimal taxAmount) {
      this.taxAmount = taxAmount;
      return this;
    }

    public Builder discountAmount(BigDecimal discountAmount) {
      this.discountAmount = discountAmount;
      return this;
    }

    public Builder imageUrl(String imageUrl) {
      this.imageUrl = imageUrl;
      return this;
    }

    public Item build() {
      if (totalAmount == null && quantity != null && unitPrice != null) {
        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity)).subtract(discount);
      }
      return new Item(this);
    }
  }
}
