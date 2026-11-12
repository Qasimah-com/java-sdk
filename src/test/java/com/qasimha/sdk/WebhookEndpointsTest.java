package com.qasimha.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qasimha.sdk.exception.ConfigurationException;
import com.qasimha.sdk.exception.NotFoundException;
import com.qasimha.sdk.exception.PermissionException;
import com.qasimha.sdk.exception.ServerException;
import com.qasimha.sdk.exception.ValidationException;
import com.qasimha.sdk.model.CreateWebhookEndpointRequest;
import com.qasimha.sdk.model.ListDeliveriesParams;
import com.qasimha.sdk.model.Page;
import com.qasimha.sdk.model.PingResult;
import com.qasimha.sdk.model.UpdateWebhookEndpointRequest;
import com.qasimha.sdk.model.WebhookDelivery;
import com.qasimha.sdk.model.WebhookEndpoint;
import com.qasimha.sdk.model.WebhookEndpointWithSecret;
import com.qasimha.sdk.model.WebhookEventTypes;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WebhookEndpointsTest {

  private static final String ENDPOINT_ID = "a81cce0b-35c7-45e0-a212-0c9f0969ddf4";
  private static final String DELIVERY_ID = "6e44f7d4-a878-4e1c-9148-e7f8745fb50d";
  private static final String URL = "https://example.com/qasimha-webhook";

  private static QasimhaClient client(MockApi api) {
    return QasimhaClient.builder().apiKey(Fixtures.TEST_KEY).baseUrl(api.baseUrl()).build();
  }

  private static CreateWebhookEndpointRequest createRequest() {
    return CreateWebhookEndpointRequest.builder()
        .url(URL)
        .description("SDK fixture endpoint")
        .addEvent(WebhookEventTypes.ORDER_CONFIRMED)
        .addEvent(WebhookEventTypes.ORDER_REFUNDED)
        .build();
  }

  @Test
  void createReturnsTheSecretOnceAndNeverLogsIt() throws Exception {
    try (MockApi api = new MockApi()) {
      RecordingLogger logger = new RecordingLogger();
      api.enqueue(201, Fixtures.read("webhook-endpoint-create.json"));
      WebhookEndpointWithSecret endpoint =
          QasimhaClient.builder()
              .apiKey(Fixtures.TEST_KEY)
              .baseUrl(api.baseUrl())
              .logger(logger)
              .build()
              .webhookEndpoints()
              .create(createRequest());

      MockApi.Recorded request = api.lastRequest();
      assertEquals("POST", request.method);
      assertEquals("/api/v1/merchant/webhooks", request.path);
      assertEquals(
          "{\"url\":\"https://example.com/qasimha-webhook\",\"description\":\"SDK fixture endpoint\","
              + "\"events\":[\"order.confirmed\",\"order.refunded\"]}",
          request.bodyText());
      assertNotNull(request.header("Idempotency-Key"));
      assertEquals("application/json; charset=utf-8", request.header("Content-Type"));

      assertEquals(ENDPOINT_ID, endpoint.getId());
      assertEquals(URL, endpoint.getUrl());
      assertEquals("SDK fixture endpoint", endpoint.getDescription());
      assertEquals(List.of("order.confirmed", "order.refunded"), endpoint.getEvents());
      assertTrue(endpoint.isActive());
      assertEquals("whsec_REDACTED_b877", endpoint.getSecret());
      assertEquals("b877", endpoint.getSecretHint());
      assertNull(endpoint.getPreviousSecretExpiresAt());
      assertEquals(Instant.parse("2026-09-25T23:42:29.5562693Z"), endpoint.getCreatedAt());
      assertNull(endpoint.getUpdatedAt());
      assertEquals("9b2016cc-4c8a-453a-9185-46220744cb18", endpoint.getRequestId());
      assertFalse(endpoint.isReplayed());

      String text = endpoint.toString();
      assertFalse(text.contains("whsec_"), text);
      assertFalse(text.contains("REDACTED"), text);
      assertTrue(text.contains(ENDPOINT_ID), text);
      assertTrue(text.contains("secretHint=b877"), text);
      assertFalse(logger.dump().contains("whsec_"), logger.dump());
      assertFalse(logger.dump().contains("REDACTED"), logger.dump());
      assertEquals("/api/v1/merchant/webhooks", logger.entries().get(0).fields.get("path"));
    }
  }

  @Test
  void createIsValidatedBeforeSending() throws Exception {
    try (MockApi api = new MockApi()) {
      WebhookEndpoints endpoints = client(api).webhookEndpoints();

      ValidationException http =
          assertThrows(
              ValidationException.class,
              () -> endpoints.create(CreateWebhookEndpointRequest.of("http://example.com/hook")));
      assertTrue(http.isClientSide());
      assertNull(http.getHttpStatus());
      assertEquals("Url", http.getErrors().get(0).getField());
      assertTrue(http.getErrors().get(0).getMessage().contains("https"));

      ValidationException blank =
          assertThrows(
              ValidationException.class,
              () -> endpoints.create(CreateWebhookEndpointRequest.of(" ")));
      assertEquals("Url", blank.getErrors().get(0).getField());

      ValidationException noHost =
          assertThrows(
              ValidationException.class,
              () -> endpoints.create(CreateWebhookEndpointRequest.of("https:///hook")));
      assertEquals("Url", noHost.getErrors().get(0).getField());

      ValidationException malformed =
          assertThrows(
              ValidationException.class,
              () -> endpoints.create(CreateWebhookEndpointRequest.of("https://exa mple.com/h")));
      assertEquals("Url", malformed.getErrors().get(0).getField());

      ValidationException events =
          assertThrows(
              ValidationException.class,
              () ->
                  endpoints.create(
                      CreateWebhookEndpointRequest.builder()
                          .url(URL)
                          .events(List.of("order.confirmed", "order.shipped", "ping"))
                          .build()));
      assertEquals(2, events.getErrors().size());
      assertEquals("Events[1]", events.getErrors().get(0).getField());
      assertEquals("Events[2]", events.getErrors().get(1).getField());
      assertTrue(events.getMessage().contains("order.shipped"), events.getMessage());

      assertThrows(ConfigurationException.class, () -> endpoints.create(null));
      assertEquals(0, api.requestCount());
    }
  }

  @Test
  void createAcceptsTheAliasAndSendsNoEventsKeyWhenUnset() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(201, Fixtures.read("webhook-endpoint-create.json"))
          .enqueue(201, Fixtures.read("webhook-endpoint-create.json"));
      WebhookEndpoints endpoints = client(api).webhookEndpoints();

      endpoints.create(
          CreateWebhookEndpointRequest.builder()
              .url(URL)
              .events(List.of("Order.Completed", " checkout_link.expired "))
              .build());
      assertEquals(
          "{\"url\":\"https://example.com/qasimha-webhook\",\"events\":[\"Order.Completed\",\" checkout_link.expired \"]}",
          api.lastRequest().bodyText());

      endpoints.create(CreateWebhookEndpointRequest.of(URL));
      assertEquals(
          "{\"url\":\"https://example.com/qasimha-webhook\"}", api.lastRequest().bodyText());
    }
  }

  @Test
  void listGetUpdateAndDelete() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(200, Fixtures.read("webhook-endpoint-list.json"))
          .enqueue(200, Fixtures.read("webhook-endpoint-get.json"))
          .enqueue(200, Fixtures.read("webhook-endpoint-update.json"))
          .enqueue(200, Fixtures.read("webhook-endpoint-delete.json"));
      WebhookEndpoints endpoints = client(api).webhookEndpoints();

      List<WebhookEndpoint> list = endpoints.list();
      assertEquals("GET", api.lastRequest().method);
      assertEquals("/api/v1/merchant/webhooks", api.lastRequest().path);
      assertNull(api.lastRequest().header("Idempotency-Key"));
      assertEquals(1, list.size());
      assertEquals(ENDPOINT_ID, list.get(0).getId());
      assertEquals("b877", list.get(0).getSecretHint());
      assertEquals("114c13ad-5036-444f-85af-0f8cf8d9fe61", list.get(0).getRequestId());
      assertFalse(list.get(0).getRaw().containsKey("secret"));
      assertFalse(list.get(0).toString().contains("whsec_"));

      WebhookEndpoint endpoint = endpoints.get(ENDPOINT_ID);
      assertEquals("/api/v1/merchant/webhooks/" + ENDPOINT_ID, api.lastRequest().path);
      assertEquals(URL, endpoint.getUrl());
      assertEquals(List.of("order.confirmed", "order.refunded"), endpoint.getEvents());
      assertTrue(endpoint.isActive());
      assertEquals(Instant.parse("2026-09-25T23:42:29.556269Z"), endpoint.getCreatedAt());
      assertNull(endpoint.getUpdatedAt());

      WebhookEndpoint updated =
          endpoints.update(
              ENDPOINT_ID,
              UpdateWebhookEndpointRequest.builder()
                  .description("SDK fixture endpoint (paused)")
                  .isActive(false)
                  .build());
      assertEquals("PUT", api.lastRequest().method);
      assertEquals("/api/v1/merchant/webhooks/" + ENDPOINT_ID, api.lastRequest().path);
      assertEquals(
          "{\"description\":\"SDK fixture endpoint (paused)\",\"isActive\":false}",
          api.lastRequest().bodyText());
      assertNotNull(api.lastRequest().header("Idempotency-Key"));
      assertEquals("application/json; charset=utf-8", api.lastRequest().header("Content-Type"));
      assertFalse(updated.isActive());
      assertEquals("SDK fixture endpoint (paused)", updated.getDescription());
      assertEquals(Instant.parse("2026-09-25T23:42:37.3003779Z"), updated.getUpdatedAt());
      assertEquals("fd0b52d5-37b9-4678-9beb-ee8d91b2def7", updated.getRequestId());

      endpoints.delete(ENDPOINT_ID);
      assertEquals("DELETE", api.lastRequest().method);
      assertEquals("/api/v1/merchant/webhooks/" + ENDPOINT_ID, api.lastRequest().path);
      assertEquals("", api.lastRequest().bodyText());
      assertNull(api.lastRequest().header("Content-Type"));
      assertNotNull(api.lastRequest().header("Idempotency-Key"));
      assertEquals(4, api.requestCount());
    }
  }

  @Test
  void updateSendsOnlyWhatWasSetAndValidatesIt() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(200, Fixtures.read("webhook-endpoint-update.json"))
          .enqueue(200, Fixtures.read("webhook-endpoint-update.json"))
          .enqueue(200, Fixtures.read("webhook-endpoint-update.json"));
      WebhookEndpoints endpoints = client(api).webhookEndpoints();

      endpoints.update(
          ENDPOINT_ID, UpdateWebhookEndpointRequest.builder().events(List.of()).build());
      assertEquals("{\"events\":[]}", api.lastRequest().bodyText());

      endpoints.update(ENDPOINT_ID, UpdateWebhookEndpointRequest.active(true));
      assertEquals("{\"isActive\":true}", api.lastRequest().bodyText());

      endpoints.update(
          ENDPOINT_ID,
          UpdateWebhookEndpointRequest.builder()
              .url("https://store.example/hooks")
              .events(Collections.singletonList(WebhookEventTypes.ORDER_DECLINED))
              .build());
      assertEquals(
          "{\"url\":\"https://store.example/hooks\",\"events\":[\"order.declined\"]}",
          api.lastRequest().bodyText());

      ValidationException url =
          assertThrows(
              ValidationException.class,
              () ->
                  endpoints.update(
                      ENDPOINT_ID,
                      UpdateWebhookEndpointRequest.builder().url("http://store.example").build()));
      assertTrue(url.isClientSide());
      assertEquals("Url", url.getErrors().get(0).getField());

      ValidationException events =
          assertThrows(
              ValidationException.class,
              () ->
                  endpoints.update(
                      ENDPOINT_ID,
                      UpdateWebhookEndpointRequest.builder().events(List.of("nope")).build()));
      assertEquals("Events[0]", events.getErrors().get(0).getField());

      assertThrows(ConfigurationException.class, () -> endpoints.update(ENDPOINT_ID, null));
      assertEquals(3, api.requestCount());
    }
  }

  @Test
  void rotateSecretPingDeliveriesAndReplay() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(200, Fixtures.read("webhook-endpoint-rotate-secret.json"))
          .enqueue(202, Fixtures.read("webhook-endpoint-ping.json"))
          .enqueue(200, Fixtures.read("webhook-endpoint-deliveries.json"))
          .enqueue(202, Fixtures.read("webhook-endpoint-replay.json"));
      WebhookEndpoints endpoints = client(api).webhookEndpoints();

      WebhookEndpointWithSecret rotated = endpoints.rotateSecret(ENDPOINT_ID);
      assertEquals("POST", api.lastRequest().method);
      assertEquals(
          "/api/v1/merchant/webhooks/" + ENDPOINT_ID + "/rotate-secret", api.lastRequest().path);
      assertEquals("", api.lastRequest().bodyText());
      assertNotNull(api.lastRequest().header("Idempotency-Key"));
      assertEquals("whsec_REDACTED_be3e", rotated.getSecret());
      assertEquals("be3e", rotated.getSecretHint());
      assertEquals(
          Instant.parse("2026-09-26T23:42:37.8012856Z"), rotated.getPreviousSecretExpiresAt());
      assertFalse(rotated.isActive());
      assertFalse(rotated.toString().contains("be3e\""), rotated.toString());
      assertFalse(rotated.toString().contains("whsec_"), rotated.toString());

      PingResult ping = endpoints.ping(ENDPOINT_ID);
      assertEquals("/api/v1/merchant/webhooks/" + ENDPOINT_ID + "/ping", api.lastRequest().path);
      assertEquals("evt_a05fd3fddfea4ac8bf76a442e3cf441d", ping.getEventId());
      assertEquals(ENDPOINT_ID, ping.getEndpointId());
      assertEquals("d7ac4270-3c42-4a92-a733-9c1d7865eefe", ping.getRequestId());

      Page<WebhookDelivery> page =
          endpoints.listDeliveries(
              ENDPOINT_ID,
              ListDeliveriesParams.builder()
                  .status(WebhookDelivery.STATUS_PENDING)
                  .page(1)
                  .pageSize(5)
                  .build());
      assertEquals("GET", api.lastRequest().method);
      assertEquals(
          "/api/v1/merchant/webhooks/" + ENDPOINT_ID + "/deliveries", api.lastRequest().path);
      assertEquals("status=Pending&page=1&pageSize=5", api.lastRequest().query);
      assertEquals(1, page.getItems().size());
      assertEquals(1, page.getPagination().getTotalItems());
      assertEquals(5, page.getPagination().getPageSize());
      assertFalse(page.getPagination().hasNext());
      WebhookDelivery delivery = page.getItems().get(0);
      assertEquals(DELIVERY_ID, delivery.getId());
      assertEquals("a05fd3fd-dfea-4ac8-bf76-a442e3cf441d", delivery.getEventId());
      assertEquals(WebhookEventTypes.PING, delivery.getEventType());
      assertNull(delivery.getOrderId());
      assertNull(delivery.getOrderReferenceId());
      assertEquals(WebhookDelivery.STATUS_PENDING, delivery.getStatus());
      assertEquals(1, delivery.getAttempts());
      assertEquals(11, delivery.getMaxAttempts());
      assertEquals(Instant.parse("2026-09-25T23:43:33.380257Z"), delivery.getNextAttemptAt());
      assertEquals(Instant.parse("2026-09-25T23:42:33.380257Z"), delivery.getLastAttemptAt());
      assertEquals(405, delivery.getLastStatusCode());
      assertEquals("HTTP 405", delivery.getLastError());
      assertTrue(delivery.getResponseSnippet().startsWith("<!doctype html>"));
      assertEquals(759, delivery.getDurationMs());
      assertNull(delivery.getSucceededAt());
      assertNull(delivery.getReplayOfId());
      assertEquals(Instant.parse("2026-09-25T23:42:32.433581Z"), delivery.getCreatedAt());
      assertEquals("01946a1d-4932-40fb-9013-45d5e94f73c6", delivery.getRequestId());

      WebhookDelivery replay = endpoints.replayDelivery(ENDPOINT_ID, DELIVERY_ID);
      assertEquals("POST", api.lastRequest().method);
      assertEquals(
          "/api/v1/merchant/webhooks/" + ENDPOINT_ID + "/deliveries/" + DELIVERY_ID + "/replay",
          api.lastRequest().path);
      assertEquals("2743654e-550b-458d-bfe2-ad3338d47807", replay.getId());
      assertEquals(DELIVERY_ID, replay.getReplayOfId());
      assertEquals(0, replay.getAttempts());
      assertEquals(WebhookDelivery.STATUS_PENDING, replay.getStatus());
      assertNull(replay.getLastStatusCode());
    }
  }

  @Test
  void listDeliveriesDefaultsToNoFilter() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
          200,
          Fixtures.envelope(
              "SUCCESS",
              "{\"data\":[],\"pagination\":{\"totalItems\":0,\"page\":1,\"pageSize\":20,\"totalPages\":0}}"));
      Page<WebhookDelivery> page = client(api).webhookEndpoints().listDeliveries(ENDPOINT_ID);
      assertNull(api.lastRequest().query);
      assertTrue(page.getItems().isEmpty());
    }
  }

  @Test
  void forbiddenWithoutTheScope() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
              403,
              Fixtures.failure(
                  "FORBIDDEN",
                  "This API key is missing the required scope: webhooks:manage",
                  null,
                  null))
          .enqueue(403, Map.of("WWW-Authenticate", "ApiKey realm=\"merchant-api\""), "");
      WebhookEndpoints endpoints = client(api).webhookEndpoints();

      PermissionException scoped =
          assertThrows(PermissionException.class, () -> endpoints.create(createRequest()));
      assertEquals(403, scoped.getHttpStatus());
      assertEquals("FORBIDDEN", scoped.getCode());
      assertTrue(scoped.getMessage().contains("webhooks:manage"));
      assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", scoped.getRequestId());

      PermissionException empty = assertThrows(PermissionException.class, endpoints::list);
      assertEquals(403, empty.getHttpStatus());
      assertNull(empty.getCode());
      assertEquals(2, api.requestCount());
    }
  }

  @Test
  void notFoundAndLimitExceeded() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(404, Fixtures.failure("NOT_FOUND", "Webhook endpoint not found", null, null))
          .enqueue(404, Fixtures.failure("NOT_FOUND", "Delivery not found", null, null))
          .enqueue(404, Fixtures.failure("NOT_FOUND", "Webhook endpoint not found", null, null))
          .enqueue(
              400,
              Fixtures.failure(
                  "LIMIT_EXCEEDED", "Maximum 5 webhook endpoints per account.", null, null));
      WebhookEndpoints endpoints = client(api).webhookEndpoints();

      NotFoundException get =
          assertThrows(NotFoundException.class, () -> endpoints.get(ENDPOINT_ID));
      assertEquals("NOT_FOUND", get.getCode());
      assertEquals("Webhook endpoint not found", get.getMessage());

      NotFoundException replay =
          assertThrows(
              NotFoundException.class, () -> endpoints.replayDelivery(ENDPOINT_ID, DELIVERY_ID));
      assertEquals("Delivery not found", replay.getMessage());

      assertThrows(NotFoundException.class, () -> endpoints.delete(ENDPOINT_ID));

      ValidationException limit =
          assertThrows(ValidationException.class, () -> endpoints.create(createRequest()));
      assertFalse(limit.isClientSide());
      assertEquals("LIMIT_EXCEEDED", limit.getCode());
      assertEquals(400, limit.getHttpStatus());
    }
  }

  @Test
  void blankIdsAreRefusedAndIdsAreEncoded() throws Exception {
    try (MockApi api = new MockApi()) {
      WebhookEndpoints endpoints = client(api).webhookEndpoints();
      assertThrows(ConfigurationException.class, () -> endpoints.get(" "));
      assertThrows(ConfigurationException.class, () -> endpoints.delete(null));
      assertThrows(ConfigurationException.class, () -> endpoints.rotateSecret(""));
      assertThrows(ConfigurationException.class, () -> endpoints.ping(null));
      assertThrows(ConfigurationException.class, () -> endpoints.listDeliveries(""));
      assertThrows(ConfigurationException.class, () -> endpoints.replayDelivery(ENDPOINT_ID, " "));
      assertThrows(
          ConfigurationException.class,
          () -> endpoints.update(" ", UpdateWebhookEndpointRequest.active(false)));
      assertEquals(0, api.requestCount());

      api.enqueue(200, Fixtures.read("webhook-endpoint-get.json"));
      endpoints.get("a b/../c");
      assertEquals("/api/v1/merchant/webhooks/a%20b%2F..%2Fc", api.lastRequest().rawPath);
    }
  }

  @Test
  void putAndDeleteRetryUnderTheSameIdempotencyKey() throws Exception {
    try (MockApi api = new MockApi()) {
      RecordingSleeper sleeper = new RecordingSleeper();
      api.enqueue(503, "")
          .enqueue(200, Fixtures.read("webhook-endpoint-update.json"))
          .enqueue(503, "")
          .enqueue(200, Fixtures.read("webhook-endpoint-delete.json"))
          .enqueue(503, "");
      WebhookEndpoints endpoints =
          QasimhaClient.builder()
              .apiKey(Fixtures.TEST_KEY)
              .baseUrl(api.baseUrl())
              .maxRetries(2)
              .sleeper(sleeper)
              .build()
              .webhookEndpoints();

      endpoints.update(ENDPOINT_ID, UpdateWebhookEndpointRequest.active(false));
      assertEquals(2, api.requestCount());
      assertEquals(
          api.requests().get(0).header("Idempotency-Key"),
          api.requests().get(1).header("Idempotency-Key"));

      endpoints.delete(ENDPOINT_ID);
      assertEquals(4, api.requestCount());
      assertEquals(
          api.requests().get(2).header("Idempotency-Key"),
          api.requests().get(3).header("Idempotency-Key"));
      assertEquals(2, sleeper.delays().size());

      ServerException e =
          assertThrows(
              ServerException.class,
              () ->
                  endpoints.update(
                      ENDPOINT_ID,
                      UpdateWebhookEndpointRequest.active(false),
                      RequestOptions.withoutIdempotency()));
      assertEquals(503, e.getHttpStatus());
      assertEquals(5, api.requestCount());
      assertNull(api.lastRequest().header("Idempotency-Key"));
    }
  }
}
