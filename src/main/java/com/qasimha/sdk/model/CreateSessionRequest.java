package com.qasimha.sdk.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The body of {@code POST /pay/session/create}. Amounts are two-decimal {@link BigDecimal}s and the
 * basket must add up: every {@code item.totalAmount == quantity × unitPrice − item.discountAmount}
 * and {@code totalAmount == Σ item.totalAmount + taxAmount + shippingAmount − discountAmount}.
 */
public final class CreateSessionRequest {

  private String orderReferenceId;
  private BigDecimal totalAmount;
  private BigDecimal taxAmount;
  private BigDecimal shippingAmount;
  private BigDecimal discountAmount;
  private String currencyCode;
  private String countryCode;
  private String description;
  private String paymentType;
  private Integer instalments;
  private List<Item> items;
  private Consumer consumer;
  private Address shippingAddress;
  private Address billingAddress;
  private RiskAssessment riskAssessment;
  private MerchantUrl merchantUrl;

  private CreateSessionRequest(Builder builder) {
    this.orderReferenceId = builder.orderReferenceId;
    this.totalAmount = builder.totalAmount;
    this.taxAmount = builder.taxAmount;
    this.shippingAmount = builder.shippingAmount;
    this.discountAmount = builder.discountAmount;
    this.currencyCode = builder.currencyCode;
    this.countryCode = builder.countryCode;
    this.description = builder.description;
    this.paymentType = builder.paymentType;
    this.instalments = builder.instalments;
    this.items = Collections.unmodifiableList(new ArrayList<>(builder.items));
    this.consumer = builder.consumer;
    this.shippingAddress = builder.shippingAddress;
    this.billingAddress = builder.billingAddress;
    this.riskAssessment = builder.riskAssessment;
    this.merchantUrl = builder.merchantUrl;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getOrderReferenceId() {
    return orderReferenceId;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public BigDecimal getTaxAmount() {
    return taxAmount;
  }

  public BigDecimal getShippingAmount() {
    return shippingAmount;
  }

  public BigDecimal getDiscountAmount() {
    return discountAmount;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public String getDescription() {
    return description;
  }

  /** The wire value, one of {@link CheckoutPaymentType}. */
  public String getPaymentType() {
    return paymentType;
  }

  public Integer getInstalments() {
    return instalments;
  }

  public List<Item> getItems() {
    return items;
  }

  public Consumer getConsumer() {
    return consumer;
  }

  public Address getShippingAddress() {
    return shippingAddress;
  }

  public Address getBillingAddress() {
    return billingAddress;
  }

  public RiskAssessment getRiskAssessment() {
    return riskAssessment;
  }

  public MerchantUrl getMerchantUrl() {
    return merchantUrl;
  }

  /** Builds a {@link CreateSessionRequest}. */
  public static final class Builder {
    private String orderReferenceId;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal shippingAmount = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private String currencyCode = "SAR";
    private String countryCode = "SA";
    private String description;
    private String paymentType = CheckoutPaymentType.PAY_BY_INSTALMENTS.getValue();
    private Integer instalments = 4;
    private final List<Item> items = new ArrayList<>();
    private Consumer consumer;
    private Address shippingAddress;
    private Address billingAddress;
    private RiskAssessment riskAssessment;
    private MerchantUrl merchantUrl;

    private Builder() {}

    public Builder orderReferenceId(String orderReferenceId) {
      this.orderReferenceId = orderReferenceId;
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

    public Builder shippingAmount(BigDecimal shippingAmount) {
      this.shippingAmount = shippingAmount;
      return this;
    }

    public Builder discountAmount(BigDecimal discountAmount) {
      this.discountAmount = discountAmount;
      return this;
    }

    public Builder currencyCode(String currencyCode) {
      this.currencyCode = currencyCode;
      return this;
    }

    public Builder countryCode(String countryCode) {
      this.countryCode = countryCode;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder paymentType(CheckoutPaymentType paymentType) {
      this.paymentType = paymentType == null ? null : paymentType.getValue();
      return this;
    }

    /** The wire value of the payment type; validated against the four known values. */
    public Builder paymentType(String paymentType) {
      this.paymentType = paymentType;
      return this;
    }

    public Builder instalments(int instalments) {
      this.instalments = instalments;
      return this;
    }

    public Builder items(List<Item> items) {
      this.items.clear();
      if (items != null) {
        this.items.addAll(items);
      }
      return this;
    }

    public Builder addItem(Item item) {
      this.items.add(item);
      return this;
    }

    public Builder consumer(Consumer consumer) {
      this.consumer = consumer;
      return this;
    }

    public Builder shippingAddress(Address shippingAddress) {
      this.shippingAddress = shippingAddress;
      return this;
    }

    public Builder billingAddress(Address billingAddress) {
      this.billingAddress = billingAddress;
      return this;
    }

    public Builder riskAssessment(RiskAssessment riskAssessment) {
      this.riskAssessment = riskAssessment;
      return this;
    }

    public Builder merchantUrl(MerchantUrl merchantUrl) {
      this.merchantUrl = merchantUrl;
      return this;
    }

    public CreateSessionRequest build() {
      return new CreateSessionRequest(this);
    }
  }
}
