package com.qasimha.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qasimha.sdk.exception.AuthenticationException;
import com.qasimha.sdk.exception.ConfigurationException;
import com.qasimha.sdk.exception.ValidationException;
import com.qasimha.sdk.model.Plan;
import com.qasimha.sdk.model.Plans;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlansTest {

  private static final String PUBLISHABLE_KEY = "pk_test_QrStUvWx5678";
  private static final String OTHER_PUBLISHABLE_KEY = "pk_test_ZzYyXxWw0001";

  private static final String NO_PLANS =
      Fixtures.envelope(
          "OK",
          "{\"merchantName\":\"Cart API Test\",\"currency\":\"SAR\",\"minAmount\":100,\"maxAmount\":4000,\"plans\":[]}");

  private static QasimhaClient.Builder builder(MockApi api) {
    return QasimhaClient.builder()
        .apiKey(Fixtures.TEST_KEY)
        .baseUrl(api.baseUrl())
        .sleeper(new RecordingSleeper());
  }

  private static QasimhaClient client(MockApi api) {
    return builder(api).publishableKey(PUBLISHABLE_KEY).build();
  }

  @Test
  void quotesThePlansFromTheFixture() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(200, Fixtures.read("widget-plans.json"));
      Plans plans = client(api).checkout().getPlans(new BigDecimal("1200.00"));

      assertEquals("Cart API Test 260924075803", plans.getMerchantName());
      assertEquals("SAR", plans.getCurrency());
      assertEquals(0, new BigDecimal("100").compareTo(plans.getMinAmount()));
      assertEquals(0, new BigDecimal("4000").compareTo(plans.getMaxAmount()));
      assertTrue(plans.isOffered());
      assertEquals(3, plans.getPlans().size());
      assertEquals("76eeb42b-b7b1-4bc7-9395-dae97215584e", plans.getRequestId());
      assertFalse(plans.isReplayed());
      assertTrue(plans.getRaw().containsKey("plans"));

      Plan two = plans.getPlans().get(0);
      assertEquals(2, two.getInstalments());
      assertEquals("monthly", two.getFrequency());
      assertEquals(0, new BigDecimal("614.38").compareTo(two.getInstalmentAmount()));
      assertEquals(0, new BigDecimal("614.38").compareTo(two.getFirstInstalmentAmount()));
      assertEquals(0, new BigDecimal("614.37").compareTo(two.getLastInstalmentAmount()));
      assertEquals(0, new BigDecimal("28.75").compareTo(two.getFeesIncludingVat()));
      assertEquals(0, new BigDecimal("1228.75").compareTo(two.getTotalPayable()));
      assertEquals(Boolean.TRUE, two.getShariaCompliant());
      assertEquals(4, plans.getPlans().get(2).getInstalments());
      assertThrows(UnsupportedOperationException.class, () -> plans.getPlans().clear());

      MockApi.Recorded request = api.lastRequest();
      assertEquals("GET", request.method);
      assertEquals("/api/v1/widget/plans", request.path);
      assertEquals("amount=1200.00&currency=SAR&lang=en", request.query);
    }
  }

  @Test
  void theRequestCarriesThePublishableKeyAndNotTheApiKey() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(200, Fixtures.read("widget-plans.json"));
      client(api).checkout().getPlans(new BigDecimal("1200"));

      MockApi.Recorded request = api.lastRequest();
      assertEquals(PUBLISHABLE_KEY, request.header("X-Public-Key"));
      assertNull(request.header("X-Api-Key"));
      assertEquals("en", request.header("Accept-Language"));
      assertEquals("application/json", request.header("Accept"));
      assertNull(request.header("Idempotency-Key"));
      assertNotNull(request.header("X-Correlation-Id"));
      assertTrue(request.header("User-Agent").startsWith("qasimha-sdk-java/"));
    }
  }

  @Test
  void languageAndKeyCanBeOverriddenPerCall() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(200, Fixtures.read("widget-plans.json"))
          .enqueue(200, Fixtures.read("widget-plans.json"));
      QasimhaClient client = client(api);

      client
          .checkout()
          .getPlans(
              new BigDecimal("1200.5"),
              GetPlansOptions.defaults().language("ar").publishableKey(OTHER_PUBLISHABLE_KEY));
      MockApi.Recorded request = api.lastRequest();
      assertEquals("amount=1200.50&currency=SAR&lang=ar", request.query);
      assertEquals("ar", request.header("Accept-Language"));
      assertEquals(OTHER_PUBLISHABLE_KEY, request.header("X-Public-Key"));
      assertNull(request.header("X-Api-Key"));

      QasimhaClient withoutKey = builder(api).language("ar").build();
      withoutKey
          .checkout()
          .getPlans(
              new BigDecimal("1200"), GetPlansOptions.defaults().publishableKey(PUBLISHABLE_KEY));
      request = api.lastRequest();
      assertEquals("amount=1200.00&currency=SAR&lang=ar", request.query);
      assertEquals(PUBLISHABLE_KEY, request.header("X-Public-Key"));
    }
  }

  @Test
  void emptyPlansAreReturnedNotThrown() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(200, NO_PLANS)
          .enqueue(
              200,
              Fixtures.envelope(
                  "OK",
                  "{\"merchantName\":null,\"currency\":\"SAR\",\"minAmount\":0,\"maxAmount\":0}"));
      Checkout checkout = client(api).checkout();

      Plans below = checkout.getPlans(new BigDecimal("50"));
      assertTrue(below.getPlans().isEmpty());
      assertFalse(below.isOffered());
      assertEquals(0, new BigDecimal("100").compareTo(below.getMinAmount()));
      assertEquals("Cart API Test", below.getMerchantName());

      Plans noPolicy = checkout.getPlans(new BigDecimal("50"));
      assertTrue(noPolicy.getPlans().isEmpty());
      assertNull(noPolicy.getMerchantName());
      assertEquals(2, api.requestCount());
    }
  }

  @Test
  void fourOhOneIsAnAuthenticationException() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
              401,
              Fixtures.failure(
                  "INVALID_PUBLIC_KEY",
                  "The publishable key is unknown, revoked or expired.",
                  null,
                  null))
          .enqueue(
              401,
              Fixtures.failure(
                  "PUBLIC_KEY_REQUIRED",
                  "Send your publishable key in the X-Public-Key header.",
                  null,
                  null));
      Checkout checkout = client(api).checkout();

      AuthenticationException invalid =
          assertThrows(
              AuthenticationException.class, () -> checkout.getPlans(new BigDecimal("1200")));
      assertEquals("INVALID_PUBLIC_KEY", invalid.getCode());
      assertEquals(401, invalid.getHttpStatus());
      assertEquals("The publishable key is unknown, revoked or expired.", invalid.getMessage());
      assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", invalid.getRequestId());
      assertFalse(invalid.toString().contains(PUBLISHABLE_KEY));

      AuthenticationException required =
          assertThrows(
              AuthenticationException.class, () -> checkout.getPlans(new BigDecimal("1200")));
      assertEquals("PUBLIC_KEY_REQUIRED", required.getCode());
      assertEquals(2, api.requestCount());
    }
  }

  @Test
  void fourHundredIsAValidationException() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
          400,
          Fixtures.failure(
              "INVALID_AMOUNT",
              "amount must be a positive number with at most two decimals.",
              null,
              null));
      ValidationException e =
          assertThrows(
              ValidationException.class,
              () -> client(api).checkout().getPlans(new BigDecimal("1200")));
      assertFalse(e.isClientSide());
      assertEquals(400, e.getHttpStatus());
      assertEquals("INVALID_AMOUNT", e.getCode());
      assertEquals(1, api.requestCount());
    }
  }

  @Test
  void missingKeyIsAConfigurationErrorBeforeAnyRequest() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(200, Fixtures.read("widget-plans.json"));
      Checkout checkout = builder(api).build().checkout();

      ConfigurationException e =
          assertThrows(
              ConfigurationException.class, () -> checkout.getPlans(new BigDecimal("1200")));
      assertTrue(e.getMessage().contains("publishableKey"), e.getMessage());
      assertThrows(
          ConfigurationException.class,
          () -> checkout.getPlans(new BigDecimal("1200"), GetPlansOptions.defaults()));
      assertThrows(ConfigurationException.class, () -> checkout.getPlans(null));
      assertEquals(0, api.requestCount());
    }
  }

  @Test
  void perCallKeyFromTheOtherEnvironmentIsRefusedBeforeAnyRequest() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(200, Fixtures.read("widget-plans.json"));
      Checkout checkout = client(api).checkout();
      ConfigurationException e =
          assertThrows(
              ConfigurationException.class,
              () ->
                  checkout.getPlans(
                      new BigDecimal("1200"),
                      GetPlansOptions.defaults().publishableKey("pk_live_AbCdEf123456")));
      assertTrue(e.getMessage().contains("pk_live_"), e.getMessage());
      assertTrue(e.getMessage().contains("sk_test_"), e.getMessage());
      assertEquals(0, api.requestCount());
    }
  }

  @Test
  void anAmountWithThreeDecimalsOrNotPositiveIsRefusedLocally() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(200, Fixtures.read("widget-plans.json"));
      Checkout checkout = client(api).checkout();

      ValidationException scale =
          assertThrows(
              ValidationException.class, () -> checkout.getPlans(new BigDecimal("1200.005")));
      assertTrue(scale.isClientSide());
      assertNull(scale.getHttpStatus());
      assertEquals("Amount", scale.getErrors().get(0).getField());
      assertTrue(scale.getMessage().contains("two decimals"), scale.getMessage());

      ValidationException zero =
          assertThrows(ValidationException.class, () -> checkout.getPlans(BigDecimal.ZERO));
      assertTrue(zero.isClientSide());
      assertEquals("Amount", zero.getErrors().get(0).getField());
      assertThrows(ValidationException.class, () -> checkout.getPlans(new BigDecimal("-1")));
      assertEquals(0, api.requestCount());

      checkout.getPlans(new BigDecimal("1200.000"));
      assertEquals("amount=1200.00&currency=SAR&lang=en", api.lastRequest().query);
    }
  }

  @Test
  void rateLimitIsRetriedLikeEveryGet() throws Exception {
    try (MockApi api = new MockApi()) {
      RecordingSleeper sleeper = new RecordingSleeper();
      api.enqueue(429, Map.of("Retry-After", "1"), "")
          .enqueue(503, "")
          .enqueue(200, Fixtures.read("widget-plans.json"));
      QasimhaClient client =
          QasimhaClient.builder()
              .apiKey(Fixtures.TEST_KEY)
              .baseUrl(api.baseUrl())
              .publishableKey(PUBLISHABLE_KEY)
              .maxRetries(2)
              .sleeper(sleeper)
              .build();
      Plans plans = client.checkout().getPlans(new BigDecimal("1200"));
      assertEquals(3, plans.getPlans().size());
      assertEquals(3, api.requestCount());
      assertEquals(2, sleeper.delays().size());
      assertEquals(1000L, sleeper.delays().get(0));
      List<MockApi.Recorded> requests = api.requests();
      assertEquals(
          requests.get(0).header("X-Correlation-Id"), requests.get(2).header("X-Correlation-Id"));
      for (MockApi.Recorded request : requests) {
        assertEquals(PUBLISHABLE_KEY, request.header("X-Public-Key"));
        assertNull(request.header("X-Api-Key"));
      }
    }
  }

  @Test
  void thePublishableKeyIsMaskedAndNeverReachesTheLog() throws Exception {
    try (MockApi api = new MockApi()) {
      RecordingLogger logger = new RecordingLogger();
      api.enqueue(503, "")
          .enqueue(
              401,
              Fixtures.failure("INVALID_PUBLIC_KEY", "The publishable key is unknown", null, null));
      QasimhaClient client =
          QasimhaClient.builder()
              .apiKey(Fixtures.TEST_KEY)
              .baseUrl(api.baseUrl())
              .publishableKey(PUBLISHABLE_KEY)
              .logger(logger)
              .sleeper(new RecordingSleeper())
              .build();

      assertEquals("pk_test_…5678", client.maskedPublishableKey());
      assertTrue(client.toString().contains("publishableKey=pk_test_…5678"), client.toString());
      assertFalse(client.toString().contains(PUBLISHABLE_KEY), client.toString());

      AuthenticationException e =
          assertThrows(
              AuthenticationException.class,
              () -> client.checkout().getPlans(new BigDecimal("1200")));
      assertFalse(e.toString().contains(PUBLISHABLE_KEY));

      String dump = logger.dump();
      assertEquals(2, logger.entries().size(), dump);
      assertFalse(dump.contains(PUBLISHABLE_KEY), dump);
      assertFalse(dump.contains("QrStUvWx"), dump);
      assertFalse(dump.contains(Fixtures.TEST_KEY), dump);
      assertFalse(dump.contains("1200"), dump);
      assertTrue(dump.contains("/api/v1/widget/plans"), dump);
      assertEquals(LogLevel.WARN, logger.entries().get(0).level);
      assertEquals("GET", logger.entries().get(1).fields.get("method"));
      assertEquals(401, logger.entries().get(1).fields.get("status"));
      assertEquals("INVALID_PUBLIC_KEY", logger.entries().get(1).fields.get("code"));
    }
  }

  @Test
  void optionsRefuseBadValues() {
    assertThrows(ConfigurationException.class, () -> GetPlansOptions.defaults().language("fr"));
    assertThrows(ConfigurationException.class, () -> GetPlansOptions.defaults().language(null));
    assertThrows(
        ConfigurationException.class, () -> GetPlansOptions.defaults().publishableKey("sk_test_a"));
    assertThrows(
        ConfigurationException.class, () -> GetPlansOptions.defaults().publishableKey("pk_test_"));
    assertThrows(
        ConfigurationException.class, () -> GetPlansOptions.defaults().publishableKey(null));
    GetPlansOptions options = GetPlansOptions.defaults().language("ar");
    assertEquals("ar", options.getLanguage());
    assertNull(options.getPublishableKey());
    assertNull(GetPlansOptions.defaults().getLanguage());
    assertEquals(PUBLISHABLE_KEY, options.publishableKey(PUBLISHABLE_KEY).getPublishableKey());
    assertEquals("ar", options.publishableKey(PUBLISHABLE_KEY).getLanguage());
  }
}
