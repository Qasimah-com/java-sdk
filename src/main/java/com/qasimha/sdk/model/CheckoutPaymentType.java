package com.qasimha.sdk.model;

import com.fasterxml.jackson.annotation.JsonValue;

/** The {@code paymentType} a checkout session is created with. */
public enum CheckoutPaymentType {
  PAY_BY_INSTALMENTS("pay_by_instalments"),
  PAY_NOW("pay_now"),
  PAY_NEXT_MONTH("pay_next_month"),
  PAY_IN_FULL("pay_in_full");

  private final String value;

  CheckoutPaymentType(String value) {
    this.value = value;
  }

  /** The wire value, for example {@code pay_by_instalments}. */
  @JsonValue
  public String getValue() {
    return value;
  }

  /** The payment type for a wire value, or null when it is not one of the four. */
  public static CheckoutPaymentType fromValue(String value) {
    for (CheckoutPaymentType type : values()) {
      if (type.value.equals(value)) {
        return type;
      }
    }
    return null;
  }
}
