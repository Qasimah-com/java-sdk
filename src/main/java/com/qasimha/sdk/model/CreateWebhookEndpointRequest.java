package com.qasimha.sdk.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The body of {@code POST /merchant/webhooks}. {@code url} must be {@code https://}; {@code events}
 * lists the {@link WebhookEventTypes} to receive and means every event when left unset.
 */
public final class CreateWebhookEndpointRequest {

  private String url;
  private String description;
  private List<String> events;

  private CreateWebhookEndpointRequest(Builder builder) {
    this.url = builder.url;
    this.description = builder.description;
    this.events =
        builder.events == null
            ? null
            : Collections.unmodifiableList(new ArrayList<>(builder.events));
  }

  public static Builder builder() {
    return new Builder();
  }

  /** An endpoint for every event. */
  public static CreateWebhookEndpointRequest of(String url) {
    return builder().url(url).build();
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

  /** Builds a {@link CreateWebhookEndpointRequest}. */
  public static final class Builder {
    private String url;
    private String description;
    private List<String> events;

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

    public Builder addEvent(String event) {
      if (this.events == null) {
        this.events = new ArrayList<>();
      }
      this.events.add(event);
      return this;
    }

    public CreateWebhookEndpointRequest build() {
      return new CreateWebhookEndpointRequest(this);
    }
  }
}
