package com.qasimha.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qasimha.sdk.exception.ApiException;
import com.qasimha.sdk.exception.ConflictException;
import com.qasimha.sdk.exception.ConnectionException;
import com.qasimha.sdk.exception.NotFoundException;
import com.qasimha.sdk.exception.RateLimitException;
import com.qasimha.sdk.exception.ServerException;
import com.qasimha.sdk.exception.ValidationException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ErrorMappingTest {

  private static QasimhaClient client(MockApi api, int maxRetries) {
    return QasimhaClient.builder()
        .apiKey(Fixtures.TEST_KEY)
        .baseUrl(api.baseUrl())
        .maxRetries(maxRetries)
        .sleeper(new RecordingSleeper())
        .build();
  }

  @Test
  void validationErrorCarriesFieldErrors() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
          400,
          Fixtures.failure(
              "VALIDATION_ERROR",
              "Validation failed",
              null,
              "[{\"field\":\"Items[0].Quantity\",\"message\":\"Must be at least 1\"},{\"field\":\"TotalAmount\",\"message\":\"Must be greater than 0\"}]"));
      ValidationException e =
          assertThrows(ValidationException.class, () -> client(api, 2).orders().get("x"));
      assertFalse(e.isClientSide());
      assertEquals(400, e.getHttpStatus());
      assertEquals("VALIDATION_ERROR", e.getCode());
      assertEquals(2, e.getErrors().size());
      assertEquals("Items[0].Quantity", e.getErrors().get(0).getField());
      assertEquals("Must be at least 1", e.getErrors().get(0).getMessage());
      assertEquals("Validation failed", e.getMessage());
      assertEquals(1, api.requestCount());
    }
  }

  @Test
  void otherFourHundredsAreValidationErrorsToo() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
          400,
          Fixtures.failure(
              "INVALID_STATUS", "Order cannot be confirmed in status Confirmed", null, null));
      ValidationException e =
          assertThrows(ValidationException.class, () -> client(api, 2).orders().capture("x"));
      assertEquals("INVALID_STATUS", e.getCode());
      assertTrue(e.getErrors().isEmpty());
    }
  }

  @Test
  void notFound() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(404, Fixtures.failure("NOT_FOUND", "Order not found", null, null));
      NotFoundException e =
          assertThrows(NotFoundException.class, () -> client(api, 2).orders().get("x"));
      assertEquals("NOT_FOUND", e.getCode());
      assertEquals(404, e.getHttpStatus());
    }
  }

  @Test
  void otherConflictsAreConflictErrors() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
          409, Fixtures.failure("MERCHANT_SUSPENDED", "This merchant is suspended", null, null));
      ConflictException e =
          assertThrows(
              ConflictException.class,
              () -> client(api, 2).checkout().createSession(Fixtures.threeLineBasket("C").build()));
      assertEquals("MERCHANT_SUSPENDED", e.getCode());
      assertEquals(ConflictException.class, e.getClass());
      assertEquals(1, api.requestCount());
    }
  }

  @Test
  void unmappedFourTwentyTwoIsAnApiError() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(422, Fixtures.failure("SOMETHING_ELSE", "Nope", null, null));
      ApiException e =
          assertThrows(
              ApiException.class,
              () -> client(api, 2).checkout().createSession(Fixtures.threeLineBasket("C").build()));
      assertEquals(ApiException.class, e.getClass());
      assertEquals(422, e.getHttpStatus());
    }
  }

  @Test
  void rateLimitCarriesRetryAfter() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(429, Map.of("Retry-After", "7", "Content-Type", "application/json"), "");
      RateLimitException e =
          assertThrows(RateLimitException.class, () -> client(api, 0).orders().counts());
      assertEquals(7L, e.getRetryAfterSeconds());
      assertEquals(429, e.getHttpStatus());
      assertTrue(e.getMessage().contains("429"));
    }
  }

  @Test
  void serverErrorsAreNeverRetriedOnFiveHundred() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(500, Fixtures.failure("INTERNAL_ERROR", "Boom", null, null));
      ServerException e =
          assertThrows(ServerException.class, () -> client(api, 3).orders().counts());
      assertEquals(500, e.getHttpStatus());
      assertEquals("INTERNAL_ERROR", e.getCode());
      assertEquals(1, api.requestCount());
    }
  }

  @Test
  void nonJsonErrorBodyIsStillMapped() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(503, Map.of("Content-Type", "text/html"), "<html>Service Unavailable</html>");
      ServerException e =
          assertThrows(ServerException.class, () -> client(api, 0).orders().counts());
      assertEquals(503, e.getHttpStatus());
      assertNull(e.getCode());
      assertTrue(e.getRawEnvelope().isEmpty());
      assertEquals("Unexpected response (HTTP 503)", e.getMessage());
    }
  }

  @Test
  void redirectsAreNotFollowed() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(302, Map.of("Location", api.baseUrl() + "/api/v1/merchant/orders/counts"), "");
      ApiException e = assertThrows(ApiException.class, () -> client(api, 2).orders().counts());
      assertEquals(302, e.getHttpStatus());
      assertEquals(1, api.requestCount());
      assertTrue(e.getMessage().contains("redirect"));
    }
  }

  @Test
  void connectionRefused() throws Exception {
    String baseUrl;
    try (MockApi api = new MockApi()) {
      baseUrl = api.baseUrl();
    }
    RecordingSleeper sleeper = new RecordingSleeper();
    QasimhaClient client =
        QasimhaClient.builder()
            .apiKey(Fixtures.TEST_KEY)
            .baseUrl(baseUrl)
            .maxRetries(1)
            .sleeper(sleeper)
            .build();
    ConnectionException e = assertThrows(ConnectionException.class, () -> client.orders().counts());
    assertNotNull(e.getCause());
    assertNull(e.getHttpStatus());
    assertEquals(1, sleeper.delays().size(), "one retry before giving up");
    assertFalse(e.getMessage().contains(Fixtures.TEST_KEY));
  }

  @Test
  void timeoutIsAConnectionError() throws Exception {
    try (ServerSocket silent =
        new ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))) {
      Thread acceptor =
          new Thread(
              () -> {
                try {
                  while (!silent.isClosed()) {
                    Socket socket = silent.accept();
                    socket.setSoLinger(false, 0);
                  }
                } catch (IOException ignored) {
                  // closed
                }
              });
      acceptor.setDaemon(true);
      acceptor.start();
      QasimhaClient client =
          QasimhaClient.builder()
              .apiKey(Fixtures.TEST_KEY)
              .baseUrl("http://127.0.0.1:" + silent.getLocalPort())
              .timeout(Duration.ofMillis(300))
              .maxRetries(0)
              .build();
      ConnectionException e =
          assertThrows(ConnectionException.class, () -> client.orders().counts());
      assertTrue(e.getCause() instanceof HttpTimeoutException, String.valueOf(e.getCause()));
    }
  }
}
