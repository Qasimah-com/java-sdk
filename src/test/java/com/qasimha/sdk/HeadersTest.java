package com.qasimha.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class HeadersTest {

  private static final Pattern UUID_V4 =
      Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");

  private static final String SESSION =
      Fixtures.envelope(
          "SESSION_CREATED",
          "{\"installmentId\":\"9a2e6c4b-1111-4c2d-8a1f-000000000001\",\"providerSessionId\":\"sess_1\",\"paymentUrl\":\"https://pay.example.test/x\",\"status\":\"New\"}");

  @Test
  void postCarriesTheFullHeaderSet() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(201, SESSION);
      QasimhaClient client =
          QasimhaClient.builder()
              .apiKey(Fixtures.TEST_KEY)
              .baseUrl(api.baseUrl())
              .language("ar")
              .userAgentSuffix("woocommerce/1.2.0")
              .build();
      client.checkout().createSession(Fixtures.threeLineBasket("CART-H-1").build());

      MockApi.Recorded request = api.lastRequest();
      assertEquals("POST", request.method);
      assertEquals("/api/v1/pay/session/create", request.path);
      assertEquals(Fixtures.TEST_KEY, request.header("X-Api-Key"));
      assertEquals("application/json", request.header("Accept"));
      assertEquals("application/json; charset=utf-8", request.header("Content-Type"));
      assertEquals("ar", request.header("Accept-Language"));
      String userAgent = request.header("User-Agent");
      assertTrue(
          userAgent.matches(
              "^qasimha-sdk-java/"
                  + Pattern.quote(Version.VERSION)
                  + " \\(java/[^;]+; [^)]+\\) woocommerce/1\\.2\\.0$"),
          userAgent);
      assertEquals(userAgent, client.getUserAgent());
      assertTrue(UUID_V4.matcher(request.header("Idempotency-Key")).matches());
      assertTrue(UUID_V4.matcher(request.header("X-Correlation-Id")).matches());
    }
  }

  @Test
  void getCarriesNoIdempotencyKeyAndDefaultsToEnglish() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(200, Fixtures.read("orders-list.json"));
      QasimhaClient client =
          QasimhaClient.builder().apiKey(Fixtures.TEST_KEY).baseUrl(api.baseUrl()).build();
      client.orders().list();

      MockApi.Recorded request = api.lastRequest();
      assertEquals("GET", request.method);
      assertNull(request.header("Idempotency-Key"));
      assertNull(request.header("Content-Type"));
      assertEquals("en", request.header("Accept-Language"));
      assertNotNull(request.header("X-Correlation-Id"));
      assertTrue(request.header("User-Agent").endsWith(")"));
    }
  }

  @Test
  void callerMayChooseOrDisableTheIdempotencyKey() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(201, SESSION).enqueue(201, SESSION);
      QasimhaClient client =
          QasimhaClient.builder().apiKey(Fixtures.TEST_KEY).baseUrl(api.baseUrl()).build();

      client
          .checkout()
          .createSession(
              Fixtures.threeLineBasket("CART-H-2").build(),
              RequestOptions.withIdempotencyKey("order-42-attempt-1"));
      assertEquals("order-42-attempt-1", api.lastRequest().header("Idempotency-Key"));

      client
          .checkout()
          .createSession(
              Fixtures.threeLineBasket("CART-H-3").build(), RequestOptions.withoutIdempotency());
      assertNull(api.lastRequest().header("Idempotency-Key"));
    }
  }
}
