package com.qasimha.sdk.model;

/** The result of {@code webhookEndpoints().ping(id)}: the queued test event. */
public final class PingResult extends ApiObject {

  private String eventId;
  private String endpointId;

  private PingResult() {}

  /** The event id ({@code evt_...}) the delivery will carry in {@code X-Qasimha-Event-Id}. */
  public String getEventId() {
    return eventId;
  }

  public String getEndpointId() {
    return endpointId;
  }
}
