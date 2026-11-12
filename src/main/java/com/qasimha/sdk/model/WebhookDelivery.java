package com.qasimha.sdk.model;

import java.time.Instant;

/**
 * One row of an endpoint's delivery log: one event to one endpoint, with the outcome of every
 * attempt so far and when the next retry is due.
 */
public final class WebhookDelivery extends ApiObject {

  /** Waiting for its next attempt, first or retry. */
  public static final String STATUS_PENDING = "Pending";

  /** The endpoint answered 2xx. */
  public static final String STATUS_SUCCEEDED = "Succeeded";

  /** Every attempt failed, or the endpoint was removed before delivery. */
  public static final String STATUS_FAILED = "Failed";

  private String id;
  private String eventId;
  private String eventType;
  private String orderId;
  private String orderReferenceId;
  private String status;
  private Integer attempts;
  private Integer maxAttempts;
  private Instant nextAttemptAt;
  private Instant lastAttemptAt;
  private Integer lastStatusCode;
  private String lastError;
  private String responseSnippet;
  private Integer durationMs;
  private Instant succeededAt;
  private String replayOfId;
  private Instant createdAt;

  private WebhookDelivery() {}

  public String getId() {
    return id;
  }

  /** The event's id as the delivery log records it (a bare GUID). */
  public String getEventId() {
    return eventId;
  }

  /** The event type, for example {@code order.confirmed} or {@code ping}. */
  public String getEventType() {
    return eventType;
  }

  /** The Qasimha order id, or null for a {@code ping}. */
  public String getOrderId() {
    return orderId;
  }

  public String getOrderReferenceId() {
    return orderReferenceId;
  }

  /** {@link #STATUS_PENDING}, {@link #STATUS_SUCCEEDED} or {@link #STATUS_FAILED}. */
  public String getStatus() {
    return status;
  }

  public Integer getAttempts() {
    return attempts;
  }

  public Integer getMaxAttempts() {
    return maxAttempts;
  }

  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }

  public Instant getLastAttemptAt() {
    return lastAttemptAt;
  }

  /** The HTTP status the endpoint last answered, or null before the first attempt. */
  public Integer getLastStatusCode() {
    return lastStatusCode;
  }

  public String getLastError() {
    return lastError;
  }

  /** The first characters of the endpoint's last response body. */
  public String getResponseSnippet() {
    return responseSnippet;
  }

  public Integer getDurationMs() {
    return durationMs;
  }

  public Instant getSucceededAt() {
    return succeededAt;
  }

  /** The delivery this one replays, or null. */
  public String getReplayOfId() {
    return replayOfId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
