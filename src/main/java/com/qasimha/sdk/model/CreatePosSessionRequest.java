package com.qasimha.sdk.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The body of {@code POST /pay/pos/create}. {@code consumer.email} is required here and the same
 * basket identities as {@link CreateSessionRequest} are checked client-side.
 */
public final class CreatePosSessionRequest {

  private String orderReferenceId;
  private BigDecimal totalAmount;
  private String currencyCode;
  private String countryCode;
  private List<Item> items;
  private Consumer consumer;

  private CreatePosSessionRequest(Builder builder) {
    this.orderReferenceId = builder.orderReferenceId;
    this.totalAmount = builder.totalAmount;
    this.currencyCode = builder.currencyCode;
    this.countryCode = builder.countryCode;
    this.items = Collections.unmodifiableList(new ArrayList<>(builder.items));
    this.consumer = builder.consumer;
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

  public Consumer getConsumer() {
    return consumer;
  }

  /** Builds a {@link CreatePosSessionRequest}. */
  public static final class Builder {
    private String orderReferenceId;
    private BigDecimal totalAmount;
    private String currencyCode = "SAR";
    private String countryCode = "SA";
    private final List<Item> items = new ArrayList<>();
    private Consumer consumer;

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

    public Builder consumer(Consumer consumer) {
      this.consumer = consumer;
      return this;
    }

    public CreatePosSessionRequest build() {
      return new CreatePosSessionRequest(this);
    }
  }
}
