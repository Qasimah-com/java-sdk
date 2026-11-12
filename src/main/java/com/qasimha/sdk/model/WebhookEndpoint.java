package com.qasimha.sdk.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * A registered webhook endpoint. The signing secret is never part of it; {@link #getSecretHint()}
 * is the last characters of the live secret so a server's copy can be told apart.
 */
public class WebhookEndpoint extends ApiObject {

  private String id;
  private String url;
  private String description;
  private List<String> events;
  private Boolean isActive;
  private String secretHint;
  private Instant previousSecretExpiresAt;
  private Instant createdAt;
  private Instant updatedAt;

  WebhookEndpoint() {}

  public String getId() {
    return id;
  }

  /** The HTTPS URL deliveries are sent to. */
  public String getUrl() {
    return url;
  }

  public String getDescription() {
    return description;
  }

  /** The subscribed event types; empty means every event. */
  public List<String> getEvents() {
    return events == null ? Collections.emptyList() : Collections.unmodifiableList(events);
  }

  /** Whether deliveries are being sent; a paused endpoint keeps its secret and its log. */
  public boolean isActive() {
    return Boolean.TRUE.equals(isActive);
  }

  /** The last characters of the live secret. */
  public String getSecretHint() {
    return secretHint;
  }

  /** Until when the previous secret still signs deliveries after a rotation, or null. */
  public Instant getPreviousSecretExpiresAt() {
    return previousSecretExpiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{id="
        + id
        + ", url="
        + url
        + ", events="
        + getEvents()
        + ", isActive="
        + isActive()
        + ", secretHint="
        + secretHint
        + '}';
  }
}
