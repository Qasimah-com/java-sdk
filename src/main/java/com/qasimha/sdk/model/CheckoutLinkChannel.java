package com.qasimha.sdk.model;

import com.fasterxml.jackson.annotation.JsonValue;

/** How a checkout link reaches the customer: an SMS link (needs a phone) or a QR code. */
public enum CheckoutLinkChannel {
  LINK("Link"),
  QR("Qr");

  private final String value;

  CheckoutLinkChannel(String value) {
    this.value = value;
  }

  /** The wire value, {@code Link} or {@code Qr}. */
  @JsonValue
  public String getValue() {
    return value;
  }
}
