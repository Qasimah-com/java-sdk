package com.qasimha.sdk.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A verified webhook delivery. Reads both the body a registered endpoint receives ({@code id},
 * {@code type}, {@code createdAt}, {@code orderId}, {@code data.order}) and the one the global URL
 * sends ({@code eventId}, {@code eventType}, {@code occurredAt}, no order id); {@link #getRaw()} is
 * the whole decoded body. Confirm the order state with {@code orders().get(id)} or {@code
 * orders().getByReference(orderReferenceId)} before acting on it.
 */
public final class WebhookEvent extends ApiObject {

  @JsonAlias("eventId")
  private String id;

  @JsonAlias("eventType")
  private String type;

  private String orderId;
  private String orderReferenceId;
  private String status;
  private BigDecimal amount;
  private String currency;

  @JsonAlias("createdAt")
  private Instant occurredAt;

  private WebhookEvent() {}

  /** The delivery id ({@code evt_...}); use it to deduplicate. */
  public String getId() {
    return id;
  }

  /** The event type, for example {@code order.completed} or {@code order.confirmed}. */
  public String getType() {
    return type;
  }

  /** The Qasimha order id; null on a delivery from the global URL, which carries none. */
  public String getOrderId() {
    return orderId;
  }

  public String getOrderReferenceId() {
    return orderReferenceId;
  }

  public String getStatus() {
    return status;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
