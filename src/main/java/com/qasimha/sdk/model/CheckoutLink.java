package com.qasimha.sdk.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * A payment-link / QR order. Create, get and list return different subsets of these members today,
 * so every one of them except {@link #getId()} may be null; {@link #getRaw()} has the full shape.
 */
public final class CheckoutLink extends ApiObject {

  private String id;
  private String url;
  private String channel;
  private BigDecimal amount;
  private String currency;
  private String phone;
  private String language;
  private String orderReferenceId;
  private String ejarContractNumber;
  private List<CheckoutLinkItem> items;
  private OrderStatus status;
  private String decisionStatus;
  private Instant expiresAt;
  private Instant createdAt;
  private Boolean completed;
  private Boolean declined;
  private Boolean waiting;

  private CheckoutLink() {}

  /** The Qasimha order id. */
  public String getId() {
    return id;
  }

  /** The hosted checkout page; returned on creation. */
  public String getUrl() {
    return url;
  }

  /** {@code Link} or {@code Qr}. */
  public String getChannel() {
    return channel;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  /** The customer phone; masked on get and list. */
  public String getPhone() {
    return phone;
  }

  public String getLanguage() {
    return language;
  }

  public String getOrderReferenceId() {
    return orderReferenceId;
  }

  public String getEjarContractNumber() {
    return ejarContractNumber;
  }

  public List<CheckoutLinkItem> getItems() {
    return items == null ? Collections.emptyList() : Collections.unmodifiableList(items);
  }

  public OrderStatus getStatus() {
    return status;
  }

  public String getDecisionStatus() {
    return decisionStatus;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  /** True once the customer finished checkout; null when the read does not report it. */
  public Boolean getCompleted() {
    return completed;
  }

  /** True when the credit decision rejected it; null when the read does not report it. */
  public Boolean getDeclined() {
    return declined;
  }

  /** True while the link is still open for checkout; only on list rows. */
  public Boolean getWaiting() {
    return waiting;
  }
}
