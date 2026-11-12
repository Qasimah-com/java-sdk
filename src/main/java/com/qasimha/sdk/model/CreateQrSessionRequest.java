package com.qasimha.sdk.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The body of {@code POST /pay/qr/create}. */
public final class CreateQrSessionRequest {

  private String orderReferenceId;
  private BigDecimal totalAmount;
  private String currencyCode;
  private String countryCode;
  private List<Item> items;

  private CreateQrSessionRequest(Builder builder) {
    this.orderReferenceId = builder.orderReferenceId;
    this.totalAmount = builder.totalAmount;
    this.currencyCode = builder.currencyCode;
    this.countryCode = builder.countryCode;
    this.items = Collections.unmodifiableList(new ArrayList<>(builder.items));
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

  public String getCurrencyCode() {
    return currencyCode;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public List<Item> getItems() {
    return items;
  }

  /** Builds a {@link CreateQrSessionRequest}. */
  public static final class Builder {
    private String orderReferenceId;
    private BigDecimal totalAmount;
    private String currencyCode = "SAR";
    private String countryCode = "SA";
    private final List<Item> items = new ArrayList<>();

    private Builder() {}

    public Builder orderReferenceId(String orderReferenceId) {
      this.orderReferenceId = orderReferenceId;
      return this;
    }

    public Builder totalAmount(BigDecimal totalAmount) {
      this.totalAmount = totalAmount;
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

    public CreateQrSessionRequest build() {
      return new CreateQrSessionRequest(this);
    }
  }
}
