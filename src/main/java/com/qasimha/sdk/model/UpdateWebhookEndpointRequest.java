package com.qasimha.sdk.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The body of {@code PUT /merchant/webhooks/{id}}. A member left unset keeps its value; an empty
 * {@code events} list means every event.
 */
public final class UpdateWebhookEndpointRequest {

  private String url;
  private String description;
  private List<String> events;
  private Boolean isActive;

  private UpdateWebhookEndpointRequest(Builder builder) {
    this.url = builder.url;
    this.description = builder.description;
    this.events =
        builder.events == null
            ? null
            : Collections.unmodifiableList(new ArrayList<>(builder.events));
    this.isActive = builder.isActive;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Pauses or resumes deliveries and changes nothing else. */
  public static UpdateWebhookEndpointRequest active(boolean isActive) {
    return builder().isActive(isActive).build();
  }

  public String getUrl() {
    return url;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getEvents() {
    return events;
  }

  public Boolean getIsActive() {
    return isActive;
  }

  /** Builds an {@link UpdateWebhookEndpointRequest}. */
  public static final class Builder {
    private String url;
    private String description;
    private List<String> events;
    private Boolean isActive;

    private Builder() {}

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder events(List<String> events) {
      this.events = events == null ? null : new ArrayList<>(events);
      return this;
    }

    public Builder isActive(boolean isActive) {
      this.isActive = isActive;
      return this;
    }

    public UpdateWebhookEndpointRequest build() {
      return new UpdateWebhookEndpointRequest(this);
    }
  }
}
