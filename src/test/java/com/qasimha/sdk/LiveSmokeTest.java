package com.qasimha.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qasimha.sdk.exception.AuthenticationException;
import com.qasimha.sdk.exception.DuplicateOrderReferenceException;
import com.qasimha.sdk.exception.NotFoundException;
import com.qasimha.sdk.exception.OrderDeclinedException;
import com.qasimha.sdk.exception.ValidationException;
import com.qasimha.sdk.model.CreateSessionRequest;
import com.qasimha.sdk.model.CreateWebhookEndpointRequest;
import com.qasimha.sdk.model.Item;
import com.qasimha.sdk.model.ListDeliveriesParams;
import com.qasimha.sdk.model.ListOrdersParams;
import com.qasimha.sdk.model.Order;
import com.qasimha.sdk.model.OrderCounts;
import com.qasimha.sdk.model.OrderSummary;
import com.qasimha.sdk.model.Page;
import com.qasimha.sdk.model.PingResult;
import com.qasimha.sdk.model.Plan;
import com.qasimha.sdk.model.Plans;
import com.qasimha.sdk.model.Session;
import com.qasimha.sdk.model.UpdateWebhookEndpointRequest;
import com.qasimha.sdk.model.WebhookDelivery;
import com.qasimha.sdk.model.WebhookEndpoint;
import com.qasimha.sdk.model.WebhookEndpointWithSecret;
import com.qasimha.sdk.model.WebhookEventTypes;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * The cart contract against a real environment. Runs only with {@code QASIMHA_API_KEY} and {@code
 * QASIMHA_BASE_URL} set; the webhook endpoint round-trip additionally needs {@code
 * QASIMHA_WEBHOOKS_API_KEY}, a key with the {@code webhooks:manage} scope, and the plan quote
 * {@code QASIMHA_PUBLISHABLE_KEY}, the account's {@code pk_test_} key. {@code
 * QASIMHA_RESOLVE_TO=127.0.0.1} points the base URL's host at a tunnel through the JDK's {@code
 * jdk.net.hosts.file}; {@code QASIMHA_TLS_INSECURE=1} trusts any certificate. Both affect this test
 * only, never the SDK.
 */
@EnabledIfEnvironmentVariable(named = "QASIMHA_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "QASIMHA_BASE_URL", matches = ".+")
@TestMethodOrder(OrderAnnotation.class)
class LiveSmokeTest {

  private static final String BASE_URL = System.getenv("QASIMHA_BASE_URL");
  private static final String STAMP =
      LocalTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HHmmss"))
          + "-"
          + Long.toString(System.nanoTime() % 100_000, 36).toUpperCase();

  private static final String WEBHOOK_URL = "https://example.com/qasimha-sdk-test";

  private static QasimhaClient client;
  private static QasimhaClient webhooksClient;
  private static String orderId;
  private static String reference;

  static {
    String resolveTo = System.getenv("QASIMHA_RESOLVE_TO");
    if (resolveTo != null && !resolveTo.isEmpty() && BASE_URL != null) {
      try {
        String host = URI.create(BASE_URL).getHost();
        Path hosts = Files.createTempFile("qasimha-hosts", ".txt");
        Files.write(hosts, (resolveTo + " " + host + "\n").getBytes(StandardCharsets.UTF_8));
        hosts.toFile().deleteOnExit();
        System.setProperty("jdk.net.hosts.file", hosts.toString());
      } catch (IOException e) {
        throw new IllegalStateException("could not write the hosts file", e);
      }
    }
    if ("1".equals(System.getenv("QASIMHA_TLS_INSECURE"))) {
      System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
    }
  }

  private static QasimhaClient client() {
    if (client == null) {
      client = build(System.getenv("QASIMHA_API_KEY"));
    }
    return client;
  }

  private static QasimhaClient webhooksClient() {
    if (webhooksClient == null) {
      webhooksClient = build(System.getenv("QASIMHA_WEBHOOKS_API_KEY"));
    }
    return webhooksClient;
  }

  private static QasimhaClient build(String apiKey) {
    QasimhaClient.Builder builder =
        QasimhaClient.builder()
            .apiKey(apiKey)
            .baseUrl(BASE_URL)
            .timeout(Duration.ofSeconds(60))
            .userAgentSuffix("live-smoke-test");
    String publishableKey = System.getenv("QASIMHA_PUBLISHABLE_KEY");
    if (publishableKey != null && !publishableKey.isEmpty()) {
      builder.publishableKey(publishableKey);
    }
    if ("1".equals(System.getenv("QASIMHA_TLS_INSECURE"))) {
      builder.httpClient(trustAllClient());
    }
    return builder.build();
  }

  private static HttpClient trustAllClient() {
    try {
      SSLContext context = SSLContext.getInstance("TLS");
      TrustManager trustAll =
          new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}

            @Override
            public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[0];
            }
          };
      context.init(null, new TrustManager[] {trustAll}, new SecureRandom());
      return HttpClient.newBuilder()
          .sslContext(context)
          .followRedirects(HttpClient.Redirect.NEVER)
          .connectTimeout(Duration.ofSeconds(60))
          .build();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static CreateSessionRequest.Builder basket(String suffix) {
    return Fixtures.threeLineBasket("CART-" + STAMP + "-" + suffix);
  }

  @Test
  @org.junit.jupiter.api.Order(1)
  void aThreeLineBasketIsPayableOrDeclinedAsItsOwnOutcome() {
    reference = "CART-" + STAMP + "-A";
    try {
      Session session = client().checkout().createSession(basket("A").build());
      orderId = session.getOrderId();
      assertTrue(session.getPaymentUrl().startsWith("https"), session.getPaymentUrl());
      assertNotNull(session.getRequestId());
    } catch (OrderDeclinedException declined) {
      orderId = declined.getOrderId();
      assertEquals("ORDER_DECLINED", declined.getCode());
      assertNotNull(orderId, "a declined order still returns its id");
    }
  }

  @Test
  @org.junit.jupiter.api.Order(2)
  void theBasketReadsBackOnTheOrder() {
    Order order = client().orders().get(orderId);
    assertEquals(reference, order.getOrderReferenceId());
    assertEquals(3, order.getItems().size());
    Item cable =
        order.getItems().stream().filter(i -> "SKU-2".equals(i.getSku())).findFirst().orElseThrow();
    assertEquals(3, cable.getQuantity());
    assertEquals(0, new BigDecimal("45.00").compareTo(cable.getUnitPrice()));
    assertTrue(order.getItems().stream().anyMatch(i -> "digital".equals(i.getType())));
    assertEquals(0, new BigDecimal("1302.10").compareTo(order.getTotalAmount()));
    assertNotNull(order.getStatus());
    assertFalse(order.getRaw().isEmpty());
  }

  @Test
  @org.junit.jupiter.api.Order(3)
  void theOrderIsFoundByItsReference() {
    Order order = client().orders().getByReference(reference);
    assertEquals(orderId, order.getId());
    assertEquals(reference, order.getOrderReferenceId());
    assertEquals(3, order.getItems().size());
    assertEquals(0, new BigDecimal("1302.10").compareTo(order.getTotalAmount()));
    assertFalse(order.getRaw().containsKey("decisionGateTrace"));

    NotFoundException unknown =
        assertThrows(
            NotFoundException.class,
            () -> client().orders().getByReference("CART/" + STAMP + "/never-created"));
    assertEquals(404, unknown.getHttpStatus());
    assertNotNull(unknown.getRequestId());
  }

  @Test
  @org.junit.jupiter.api.Order(4)
  void theOrderIsListedAndCounted() {
    Page<OrderSummary> page = client().orders().list(ListOrdersParams.search(reference));
    assertTrue(
        page.getItems().stream().anyMatch(o -> orderId.equals(o.getId())), "order is listed");
    assertTrue(page.getPagination().getTotalItems() >= 1);
    OrderCounts counts = client().orders().counts();
    assertTrue(counts.getAll() >= 1);
  }

  @Test
  @org.junit.jupiter.api.Order(5)
  void aBasketThatDoesNotAddUpIsRefusedLocallyAndByTheApi() {
    ValidationException local =
        assertThrows(
            ValidationException.class,
            () ->
                client()
                    .checkout()
                    .createSession(
                        basket("C")
                            .totalAmount(new BigDecimal("10.00"))
                            .taxAmount(BigDecimal.ZERO)
                            .shippingAmount(BigDecimal.ZERO)
                            .discountAmount(BigDecimal.ZERO)
                            .items(
                                List.of(
                                    Fixtures.item(
                                        "SKU-9", "Laptop", Item.TYPE_PHYSICAL, 1, "5000.00")))
                            .build()));
    assertTrue(local.isClientSide());
    assertEquals("TotalAmount", local.getErrors().get(0).getField());

    ValidationException remote =
        assertThrows(
            ValidationException.class,
            () -> client().checkout().createSession(basket("H").currencyCode("USD").build()));
    assertFalse(remote.isClientSide());
    assertEquals(400, remote.getHttpStatus());
    assertEquals("VALIDATION_ERROR", remote.getCode());
  }

  @Test
  @org.junit.jupiter.api.Order(6)
  void theSameReferenceTwiceIsADuplicate() {
    DuplicateOrderReferenceException e =
        assertThrows(
            DuplicateOrderReferenceException.class,
            () -> client().checkout().createSession(basket("A").build()));
    assertEquals(orderId, e.getExistingOrderId());
    assertEquals(409, e.getHttpStatus());
  }

  @Test
  @org.junit.jupiter.api.Order(7)
  void anArabicProductNameIsPreserved() {
    String name = "سماعات لاسلكية";
    CreateSessionRequest request =
        basket("G")
            .totalAmount(new BigDecimal("250.00"))
            .taxAmount(BigDecimal.ZERO)
            .shippingAmount(BigDecimal.ZERO)
            .discountAmount(BigDecimal.ZERO)
            .items(List.of(Fixtures.item("SKU-AR", name, Item.TYPE_PHYSICAL, 1, "250.00")))
            .build();
    String id;
    try {
      id = client().checkout().createSession(request).getOrderId();
    } catch (OrderDeclinedException declined) {
      id = declined.getOrderId();
    }
    Order order = client().orders().get(id);
    assertEquals(name, order.getItems().get(0).getName());
  }

  @Test
  @org.junit.jupiter.api.Order(8)
  @EnabledIfEnvironmentVariable(named = "QASIMHA_WEBHOOKS_API_KEY", matches = ".+")
  void aWebhookEndpointRoundTrip() throws InterruptedException {
    WebhookEndpoints endpoints = webhooksClient().webhookEndpoints();
    for (WebhookEndpoint stale : endpoints.list()) {
      if (WEBHOOK_URL.equals(stale.getUrl())) {
        endpoints.delete(stale.getId());
      }
    }

    WebhookEndpointWithSecret created =
        endpoints.create(
            CreateWebhookEndpointRequest.builder()
                .url(WEBHOOK_URL)
                .description("SDK live smoke " + STAMP)
                .addEvent(WebhookEventTypes.ORDER_CONFIRMED)
                .addEvent(WebhookEventTypes.ORDER_REFUNDED)
                .build());
    String id = created.getId();
    try {
      assertNotNull(id);
      assertTrue(created.getSecret().startsWith("whsec_"), "secret prefix");
      assertTrue(created.getSecret().endsWith(created.getSecretHint()), "hint is the tail");
      assertFalse(created.toString().contains(created.getSecret()));
      assertTrue(created.isActive());
      assertEquals(WEBHOOK_URL, created.getUrl());
      assertEquals(List.of("order.confirmed", "order.refunded"), created.getEvents());
      assertNotNull(created.getRequestId());

      WebhookEndpoint read = endpoints.get(id);
      assertEquals(WEBHOOK_URL, read.getUrl());
      assertEquals(created.getSecretHint(), read.getSecretHint());
      assertFalse(read.getRaw().containsKey("secret"));
      assertTrue(endpoints.list().stream().anyMatch(e -> id.equals(e.getId())), "listed");

      PingResult ping = endpoints.ping(id);
      assertEquals(id, ping.getEndpointId());
      assertTrue(ping.getEventId().startsWith("evt_"), ping.getEventId());

      Page<WebhookDelivery> deliveries = endpoints.listDeliveries(id);
      for (int i = 0; i < 10 && deliveries.getItems().isEmpty(); i++) {
        Thread.sleep(1000);
        deliveries = endpoints.listDeliveries(id);
      }
      assertNotNull(deliveries.getPagination());
      if (!deliveries.getItems().isEmpty()) {
        WebhookDelivery delivery = deliveries.getItems().get(0);
        assertEquals(WebhookEventTypes.PING, delivery.getEventType());
        assertNull(delivery.getOrderId());
        assertNotNull(delivery.getStatus());
        WebhookDelivery replay = endpoints.replayDelivery(id, delivery.getId());
        assertEquals(delivery.getId(), replay.getReplayOfId());
        assertEquals(WebhookDelivery.STATUS_PENDING, replay.getStatus());
        Page<WebhookDelivery> pending =
            endpoints.listDeliveries(
                id, ListDeliveriesParams.builder().status(WebhookDelivery.STATUS_PENDING).build());
        assertNotNull(pending.getPagination());
      }

      WebhookEndpoint paused =
          endpoints.update(
              id,
              UpdateWebhookEndpointRequest.builder()
                  .description("SDK live smoke " + STAMP + " (paused)")
                  .isActive(false)
                  .build());
      assertFalse(paused.isActive());
      assertEquals("SDK live smoke " + STAMP + " (paused)", paused.getDescription());
      assertNotNull(paused.getUpdatedAt());

      WebhookEndpointWithSecret rotated = endpoints.rotateSecret(id);
      assertTrue(rotated.getSecret().startsWith("whsec_"));
      assertFalse(rotated.getSecret().equals(created.getSecret()), "a new secret");
      assertTrue(rotated.getSecret().endsWith(rotated.getSecretHint()));
      assertNotNull(rotated.getPreviousSecretExpiresAt());
      assertFalse(rotated.isActive());
      assertFalse(rotated.toString().contains(rotated.getSecret()));
    } finally {
      endpoints.delete(id);
    }
    NotFoundException gone = assertThrows(NotFoundException.class, () -> endpoints.get(id));
    assertEquals(404, gone.getHttpStatus());
  }

  @Test
  @org.junit.jupiter.api.Order(9)
  @EnabledIfEnvironmentVariable(named = "QASIMHA_PUBLISHABLE_KEY", matches = ".+")
  void plansAreQuotedForAnAmountAndEmptyBelowTheMinimum() {
    Plans quoted = client().checkout().getPlans(new BigDecimal("1200.00"));
    assertEquals("SAR", quoted.getCurrency());
    assertNotNull(quoted.getRequestId());
    assertTrue(quoted.isOffered(), quoted.getRaw().toString());
    assertEquals(3, quoted.getPlans().size(), quoted.getRaw().toString());
    for (Plan plan : quoted.getPlans()) {
      assertTrue(plan.getInstalments() >= 2, String.valueOf(plan.getInstalments()));
      assertEquals("monthly", plan.getFrequency());
      assertTrue(plan.getInstalmentAmount().signum() > 0);
      assertTrue(plan.getFirstInstalmentAmount().signum() > 0);
      assertTrue(plan.getLastInstalmentAmount().signum() > 0);
      assertTrue(plan.getFeesIncludingVat().signum() >= 0);
      assertTrue(plan.getTotalPayable().compareTo(new BigDecimal("1200.00")) >= 0);
      assertEquals(
          0,
          new BigDecimal("1200.00")
              .add(plan.getFeesIncludingVat())
              .compareTo(plan.getTotalPayable()));
    }

    Plans below =
        client()
            .checkout()
            .getPlans(new BigDecimal("50"), GetPlansOptions.defaults().language("ar"));
    assertTrue(below.getMinAmount().compareTo(new BigDecimal("50")) > 0, "50 is below the minimum");
    assertFalse(below.isOffered());
    assertTrue(below.getPlans().isEmpty());

    AuthenticationException unknown =
        assertThrows(
            AuthenticationException.class,
            () ->
                client()
                    .checkout()
                    .getPlans(
                        new BigDecimal("1200.00"),
                        GetPlansOptions.defaults().publishableKey("pk_test_NeverIssued0000")));
    assertEquals("INVALID_PUBLIC_KEY", unknown.getCode());
    assertEquals(401, unknown.getHttpStatus());
  }
}
