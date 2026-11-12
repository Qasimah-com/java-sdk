package com.qasimha.sdk.model;

import java.math.BigDecimal;
import java.time.Instant;

/** One row of {@code orders().list(...)}. */
public final class OrderSummary extends ApiObject {

  private String id;
  private String orderReferenceId;
  private OrderStatus status;
  private String policyCode;
  private String policyType;
  private BigDecimal totalAmount;
  private String currencyCode;
  private Instant createdAt;
  private String customerId;
  private String customerName;
  private String customerEmail;
  private String customerPhone;
  private Integer instalments;
  private OrderChannel channel;
  private BigDecimal capturedAmount;
  private BigDecimal refundedAmount;
  private String ejarContractNumber;

  private OrderSummary() {}

  public String getId() {
    return id;
  }

  public String getOrderReferenceId() {
    return orderReferenceId;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public String getPolicyCode() {
    return policyCode;
  }

  public String getPolicyType() {
    return policyType;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public String getCustomerId() {
    return customerId;
  }

  public String getCustomerName() {
    return customerName;
  }

  public String getCustomerEmail() {
    return customerEmail;
  }

  public String getCustomerPhone() {
    return customerPhone;
  }

  public Integer getInstalments() {
    return instalments;
  }

  public OrderChannel getChannel() {
    return channel;
  }

  public BigDecimal getCapturedAmount() {
    return capturedAmount;
  }

  public BigDecimal getRefundedAmount() {
    return refundedAmount;
  }

  public String getEjarContractNumber() {
    return ejarContractNumber;
  }
}
