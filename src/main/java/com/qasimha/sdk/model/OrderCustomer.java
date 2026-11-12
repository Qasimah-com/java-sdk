package com.qasimha.sdk.model;

/** The customer of an order, from {@code data.customer}. */
public final class OrderCustomer {

  private String id;
  private String name;
  private String email;
  private String phone;
  private String nationalId;

  private OrderCustomer() {}

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getPhone() {
    return phone;
  }

  public String getNationalId() {
    return nationalId;
  }
}
