package com.qasimha.sdk.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The body of {@code POST /merchant/checkout-links}. {@code channel} {@link
 * CheckoutLinkChannel#LINK} needs a {@code phone}; {@code ejarContractNumber} is required for a
 * real-estate merchant and ignored otherwise.
 */
public final class CreateCheckoutLinkRequest {

  private BigDecimal amount;
  private CheckoutLinkChannel channel;
  private String phone;
  private String language;
  private String orderReferenceId;
  private List<CheckoutLinkItem> items;
  private String ejarContractNumber;

  private CreateCheckoutLinkRequest(Builder builder) {
    this.amount = builder.amount;
    this.channel = builder.channel;
    this.phone = builder.phone;
    this.language = builder.language;
    this.orderReferenceId = builder.orderReferenceId;
    this.items =
        builder.items == null ? null : Collections.unmodifiableList(new ArrayList<>(builder.items));
    this.ejarContractNumber = builder.ejarContractNumber;
  }

  public static Builder builder() {
    return new Builder();
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public CheckoutLinkChannel getChannel() {
    return channel;
  }

  public String getPhone() {
    return phone;
  }

  /** {@code ar} or {@code en}: the language the customer is contacted in. */
  public String getLanguage() {
    return language;
  }

  public String getOrderReferenceId() {
    return orderReferenceId;
  }

  public List<CheckoutLinkItem> getItems() {
    return items;
  }

  public String getEjarContractNumber() {
    return ejarContractNumber;
  }

  /** Builds a {@link CreateCheckoutLinkRequest}. */
  public static final class Builder {
    private BigDecimal amount;
    private CheckoutLinkChannel channel = CheckoutLinkChannel.LINK;
    private String phone;
    private String language;
    private String orderReferenceId;
    private List<CheckoutLinkItem> items;
    private String ejarContractNumber;

    private Builder() {}

    public Builder amount(BigDecimal amount) {
      this.amount = amount;
      return this;
    }

    public Builder channel(CheckoutLinkChannel channel) {
      this.channel = channel;
      return this;
    }

    public Builder phone(String phone) {
      this.phone = phone;
      return this;
    }

    public Builder language(String language) {
      this.language = language;
      return this;
    }

    public Builder orderReferenceId(String orderReferenceId) {
      this.orderReferenceId = orderReferenceId;
      return this;
    }

    public Builder items(List<CheckoutLinkItem> items) {
      this.items = items == null ? null : new ArrayList<>(items);
      return this;
    }

    public Builder addItem(CheckoutLinkItem item) {
      if (this.items == null) {
        this.items = new ArrayList<>();
      }
      this.items.add(item);
      return this;
    }

    public Builder ejarContractNumber(String ejarContractNumber) {
      this.ejarContractNumber = ejarContractNumber;
      return this;
    }

    public CreateCheckoutLinkRequest build() {
      return new CreateCheckoutLinkRequest(this);
    }
  }
}
