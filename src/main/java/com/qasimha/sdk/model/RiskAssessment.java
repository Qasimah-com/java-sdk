package com.qasimha.sdk.model;

/** Optional signals about the shopper that help the credit decision. */
public final class RiskAssessment {

  private String customerDob;
  private String customerGender;
  private Boolean isExistingCustomer;
  private Boolean orderIsDigital;
  private Integer totalOrderCount;
  private String dateOfFirstTransaction;

  private RiskAssessment() {}

  private RiskAssessment(Builder builder) {
    this.customerDob = builder.customerDob;
    this.customerGender = builder.customerGender;
    this.isExistingCustomer = builder.isExistingCustomer;
    this.orderIsDigital = builder.orderIsDigital;
    this.totalOrderCount = builder.totalOrderCount;
    this.dateOfFirstTransaction = builder.dateOfFirstTransaction;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getCustomerDob() {
    return customerDob;
  }

  public String getCustomerGender() {
    return customerGender;
  }

  public Boolean getIsExistingCustomer() {
    return isExistingCustomer;
  }

  public Boolean getOrderIsDigital() {
    return orderIsDigital;
  }

  public Integer getTotalOrderCount() {
    return totalOrderCount;
  }

  public String getDateOfFirstTransaction() {
    return dateOfFirstTransaction;
  }

  /** Builds a {@link RiskAssessment}. */
  public static final class Builder {
    private String customerDob;
    private String customerGender;
    private Boolean isExistingCustomer;
    private Boolean orderIsDigital;
    private Integer totalOrderCount;
    private String dateOfFirstTransaction;

    private Builder() {}

    public Builder customerDob(String customerDob) {
      this.customerDob = customerDob;
      return this;
    }

    public Builder customerGender(String customerGender) {
      this.customerGender = customerGender;
      return this;
    }

    public Builder isExistingCustomer(boolean isExistingCustomer) {
      this.isExistingCustomer = isExistingCustomer;
      return this;
    }

    public Builder orderIsDigital(boolean orderIsDigital) {
      this.orderIsDigital = orderIsDigital;
      return this;
    }

    public Builder totalOrderCount(int totalOrderCount) {
      this.totalOrderCount = totalOrderCount;
      return this;
    }

    public Builder dateOfFirstTransaction(String dateOfFirstTransaction) {
      this.dateOfFirstTransaction = dateOfFirstTransaction;
      return this;
    }

    public RiskAssessment build() {
      return new RiskAssessment(this);
    }
  }
}
