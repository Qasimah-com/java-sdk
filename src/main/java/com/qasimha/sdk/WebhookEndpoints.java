package com.qasimha.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.qasimha.sdk.internal.HttpResult;
import com.qasimha.sdk.internal.HttpTransport;
import com.qasimha.sdk.internal.WebhookEndpointValidator;
import com.qasimha.sdk.model.CreateWebhookEndpointRequest;
import com.qasimha.sdk.model.ListDeliveriesParams;
import com.qasimha.sdk.model.Page;
import com.qasimha.sdk.model.PingResult;
import com.qasimha.sdk.model.UpdateWebhookEndpointRequest;
import com.qasimha.sdk.model.WebhookDelivery;
import com.qasimha.sdk.model.WebhookEndpoint;
import com.qasimha.sdk.model.WebhookEndpointWithSecret;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Your account's webhook endpoints: where signed events are delivered, the secret that signs them
 * and the delivery log. Every method needs the {@code webhooks:manage} scope; a key without it gets
 * a {@link com.qasimha.sdk.exception.PermissionException}. Up to five endpoints per account.
 */
public final class WebhookEndpoints {

  private static final String PATH = "/merchant/webhooks";

  private final HttpTransport transport;

  WebhookEndpoints(HttpTransport transport) {
    this.transport = transport;
  }

  /**
   * Registers an endpoint and returns it with its signing secret, which is readable only in this
   * response. {@code url} must be {@code https://} and {@code events} must be {@link
   * com.qasimha.sdk.model.WebhookEventTypes}; both are checked before sending.
   */
  public WebhookEndpointWithSecret create(CreateWebhookEndpointRequest request) {
    return create(request, RequestOptions.defaults());
  }

  /** Registers an endpoint and returns it with its signing secret. */
  public WebhookEndpointWithSecret create(
      CreateWebhookEndpointRequest request, RequestOptions options) {
    ResourceSupport.requireNonNull(request, "request");
    WebhookEndpointValidator.validate(request);
    HttpResult result = transport.post(PATH, request, options);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, WebhookEndpointWithSecret.class);
  }

  /** Every endpoint registered for the account, newest first. */
  public List<WebhookEndpoint> list() {
    HttpResult result = transport.get(PATH, null);
    ResourceSupport.ensureSuccess(transport, result);
    JsonNode data = ResourceSupport.data(result);
    JsonNode rows = data != null && data.isArray() ? data : null;
    return ResourceSupport.readList(rows, WebhookEndpoint.class, result);
  }

  /** One endpoint. */
  public WebhookEndpoint get(String id) {
    HttpResult result = transport.get(PATH + "/" + ResourceSupport.requireId(id, "id"), null);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, WebhookEndpoint.class);
  }

  /** Changes the URL, description, events or active flag; unset members keep their value. */
  public WebhookEndpoint update(String id, UpdateWebhookEndpointRequest request) {
    return update(id, request, RequestOptions.defaults());
  }

  /** Changes the URL, description, events or active flag; unset members keep their value. */
  public WebhookEndpoint update(
      String id, UpdateWebhookEndpointRequest request, RequestOptions options) {
    ResourceSupport.requireNonNull(request, "request");
    WebhookEndpointValidator.validate(request);
    HttpResult result =
        transport.put(PATH + "/" + ResourceSupport.requireId(id, "id"), request, options);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, WebhookEndpoint.class);
  }

  /** Deletes an endpoint; deliveries still waiting for a retry end as failed. */
  public void delete(String id) {
    delete(id, RequestOptions.defaults());
  }

  /** Deletes an endpoint; deliveries still waiting for a retry end as failed. */
  public void delete(String id, RequestOptions options) {
    HttpResult result = transport.delete(PATH + "/" + ResourceSupport.requireId(id, "id"), options);
    ResourceSupport.ensureSuccess(transport, result);
  }

  /**
   * Issues a new secret and returns it once. Until {@link
   * WebhookEndpoint#getPreviousSecretExpiresAt()} every delivery is signed with both secrets, so
   * switch your server over within that window.
   */
  public WebhookEndpointWithSecret rotateSecret(String id) {
    return rotateSecret(id, RequestOptions.defaults());
  }

  /** Issues a new secret and returns it once. */
  public WebhookEndpointWithSecret rotateSecret(String id, RequestOptions options) {
    HttpResult result =
        transport.post(
            PATH + "/" + ResourceSupport.requireId(id, "id") + "/rotate-secret", null, options);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, WebhookEndpointWithSecret.class);
  }

  /** Queues a signed {@code ping} event to the endpoint, to prove its signature verification. */
  public PingResult ping(String id) {
    return ping(id, RequestOptions.defaults());
  }

  /** Queues a signed {@code ping} event to the endpoint. */
  public PingResult ping(String id, RequestOptions options) {
    HttpResult result =
        transport.post(PATH + "/" + ResourceSupport.requireId(id, "id") + "/ping", null, options);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, PingResult.class);
  }

  /** The first page of the endpoint's delivery log, newest first. */
  public Page<WebhookDelivery> listDeliveries(String id) {
    return listDeliveries(id, ListDeliveriesParams.all());
  }

  /** The delivery log filtered by status and paginated, newest first. */
  public Page<WebhookDelivery> listDeliveries(String id, ListDeliveriesParams params) {
    ListDeliveriesParams p = params == null ? ListDeliveriesParams.all() : params;
    Map<String, String> query = new LinkedHashMap<>();
    query.put("status", p.getStatus());
    query.put("page", p.getPage() == null ? null : p.getPage().toString());
    query.put("pageSize", p.getPageSize() == null ? null : p.getPageSize().toString());
    HttpResult result =
        transport.get(PATH + "/" + ResourceSupport.requireId(id, "id") + "/deliveries", query);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readPage(result, WebhookDelivery.class);
  }

  /** Queues the same event again as a new delivery whose {@code replayOfId} names the original. */
  public WebhookDelivery replayDelivery(String id, String deliveryId) {
    return replayDelivery(id, deliveryId, RequestOptions.defaults());
  }

  /** Queues the same event again as a new delivery. */
  public WebhookDelivery replayDelivery(String id, String deliveryId, RequestOptions options) {
    HttpResult result =
        transport.post(
            PATH
                + "/"
                + ResourceSupport.requireId(id, "id")
                + "/deliveries/"
                + ResourceSupport.requireId(deliveryId, "deliveryId")
                + "/replay",
            null,
            options);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, WebhookDelivery.class);
  }
}
