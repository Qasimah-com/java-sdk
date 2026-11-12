package com.qasimha.sdk.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * An order in full, from {@code orders().get(id)}. The API returns the whole persistence entity;
 * this model is the curated view and {@link #getRaw()} keeps everything else reachable.
 */
public final class Order extends ApiObject {

  private String id;
  private String orderReferenceId;
  private OrderStatus status;
  private PaymentType paymentType;
  private OrderChannel channel;
  private String checkoutChannel;
  private BigDecimal totalAmount;
  private BigDecimal taxAmount;
  private BigDecimal shippingAmount;
  private BigDecimal discountAmount;
  private String currencyCode;
  private String countryCode;
  private String description;
  private Integer instalments;
  private BigDecimal downPaymentAmount;
  private BigDecimal approvedAmount;
  private BigDecimal capturedAmount;
  private BigDecimal fulfilledAmount;
  private BigDecimal refundedAmount;
  private BigDecimal refundableAmount;
  private BigDecimal collectedAmount;
  private BigDecimal voidedAmount;
  private BigDecimal reversedAmount;
  private String paymentUrl;
  private String providerSessionId;
  private String providerOrderId;
  private Instant createdAt;
  private Instant confirmedAt;
  private Instant fulfilledAt;
  private Instant voidedAt;
  private Instant reversedAt;
  private Instant settledAt;
  private Instant checkoutExpiresAt;
  private Instant expiresAt;
  private String ejarContractNumber;
  private String notificationLanguage;
  private String contractNumber;
  private OrderCustomer customer;
  private List<Item> items;
  private List<Installment> installments;
  private List<Fulfillment> fulfillments;
  private List<Reversal> reversals;
  private List<OrderVoid> voids;
  private List<StatusChange> statusHistory;

  private Order() {}

  public String getId() {
    return id;
  }

  public String getOrderReferenceId() {
    return orderReferenceId;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public PaymentType getPaymentType() {
    return paymentType;
  }

  public OrderChannel getChannel() {
    return channel;
  }

  public String getCheckoutChannel() {
    return checkoutChannel;
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

  public Integer getInstalments() {
    return instalments;
  }

  public BigDecimal getDownPaymentAmount() {
    return downPaymentAmount;
  }

  public BigDecimal getApprovedAmount() {
    return approvedAmount;
  }

  public BigDecimal getCapturedAmount() {
    return capturedAmount;
  }

  public BigDecimal getFulfilledAmount() {
    return fulfilledAmount;
  }

  public BigDecimal getRefundedAmount() {
    return refundedAmount;
  }

  /** The customer's paid instalments minus refunds so far. */
  public BigDecimal getRefundableAmount() {
    return refundableAmount;
  }

  public BigDecimal getCollectedAmount() {
    return collectedAmount;
  }

  public BigDecimal getVoidedAmount() {
    return voidedAmount;
  }

  public BigDecimal getReversedAmount() {
    return reversedAmount;
  }

  public String getPaymentUrl() {
    return paymentUrl;
  }

  public String getProviderSessionId() {
    return providerSessionId;
  }

  public String getProviderOrderId() {
    return providerOrderId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getConfirmedAt() {
    return confirmedAt;
  }

  public Instant getFulfilledAt() {
    return fulfilledAt;
  }

  public Instant getVoidedAt() {
    return voidedAt;
  }

  public Instant getReversedAt() {
    return reversedAt;
  }

  public Instant getSettledAt() {
    return settledAt;
  }

  public Instant getCheckoutExpiresAt() {
    return checkoutExpiresAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public String getEjarContractNumber() {
    return ejarContractNumber;
  }

  public String getNotificationLanguage() {
    return notificationLanguage;
  }

  public String getContractNumber() {
    return contractNumber;
  }

  public OrderCustomer getCustomer() {
    return customer;
  }

  public List<Item> getItems() {
    return nullSafe(items);
  }

  public List<Installment> getInstallments() {
    return nullSafe(installments);
  }

  public List<Fulfillment> getFulfillments() {
    return nullSafe(fulfillments);
  }

  public List<Reversal> getReversals() {
    return nullSafe(reversals);
  }

  public List<OrderVoid> getVoids() {
    return nullSafe(voids);
  }

  public List<StatusChange> getStatusHistory() {
    return nullSafe(statusHistory);
  }

  private static <T> List<T> nullSafe(List<T> list) {
    return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
  }
}
