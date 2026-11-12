package com.qasimha.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qasimha.sdk.exception.ValidationException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LoggingTest {

  private static final Set<String> ALLOWED_FIELDS =
      Set.of("method", "path", "status", "durationMs", "requestId", "attempt", "code", "delayMs");

  @Test
  void logsCarryOnlyTheDocumentedFieldsAndNoSecretsOrPii() throws Exception {
    try (MockApi api = new MockApi()) {
      RecordingLogger logger = new RecordingLogger();
      api.enqueue(503, "")
          .enqueue(
              400,
              Fixtures.failure(
                  "VALIDATION_ERROR",
                  "Validation failed",
                  null,
                  "[{\"field\":\"Consumer.PhoneNumber\",\"message\":\"Invalid\"}]"));
      QasimhaClient client =
          QasimhaClient.builder()
              .apiKey(Fixtures.TEST_KEY)
              .baseUrl(api.baseUrl())
              .logger(logger)
              .sleeper(new RecordingSleeper())
              .build();
      ValidationException e =
          assertThrows(
              ValidationException.class,
              () ->
                  client.checkout().createSession(Fixtures.threeLineBasket("CART-LOG-1").build()));

      assertFalse(logger.entries().isEmpty());
      for (RecordingLogger.Entry entry : logger.entries()) {
        assertTrue(ALLOWED_FIELDS.containsAll(entry.fields.keySet()), entry.toString());
      }
      String dump = logger.dump();
      assertFalse(dump.contains(Fixtures.TEST_KEY), dump);
      assertFalse(dump.contains("Noura"), dump);
      assertFalse(dump.contains("+966512349999"), dump);
      assertFalse(dump.contains("cart-shopper"), dump);
      assertFalse(dump.contains("CART-LOG-1"), dump);
      assertTrue(dump.contains("/api/v1/pay/session/create"), dump);

      RecordingLogger.Entry retry = logger.entries().get(0);
      assertEquals(LogLevel.WARN, retry.level);
      assertEquals(503, retry.fields.get("status"));
      assertEquals(1, retry.fields.get("attempt"));
      assertTrue(retry.fields.containsKey("delayMs"));

      RecordingLogger.Entry done = logger.entries().get(1);
      assertEquals(LogLevel.DEBUG, done.level);
      assertEquals(400, done.fields.get("status"));
      assertEquals(2, done.fields.get("attempt"));
      assertEquals("VALIDATION_ERROR", done.fields.get("code"));
      assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", done.fields.get("requestId"));
      assertEquals("POST", done.fields.get("method"));
      assertTrue(done.fields.containsKey("durationMs"));

      assertFalse(e.toString().contains(Fixtures.TEST_KEY));
      assertTrue(e.toString().contains("VALIDATION_ERROR"));
      assertTrue(e.toString().contains("Consumer.PhoneNumber"));
    }
  }

  @Test
  void searchTermsNeverReachTheLog() throws Exception {
    try (MockApi api = new MockApi()) {
      RecordingLogger logger = new RecordingLogger();
      api.enqueue(200, Fixtures.read("orders-list.json"));
      QasimhaClient.builder()
          .apiKey(Fixtures.TEST_KEY)
          .baseUrl(api.baseUrl())
          .logger(logger)
          .build()
          .orders()
          .list(com.qasimha.sdk.model.ListOrdersParams.search("+966512349999"));
      assertFalse(logger.dump().contains("966512349999"), logger.dump());
      assertEquals("/api/v1/merchant/orders", logger.entries().get(0).fields.get("path"));
    }
  }

  @Test
  void noLoggerMeansNoLogging() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(200, Fixtures.read("orders-list.json"));
      QasimhaClient.builder()
          .apiKey(Fixtures.TEST_KEY)
          .baseUrl(api.baseUrl())
          .build()
          .orders()
          .list();
      assertEquals(1, api.requestCount());
    }
  }
}
