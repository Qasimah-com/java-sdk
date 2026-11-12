package com.qasimha.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qasimha.sdk.exception.ConflictException;
import com.qasimha.sdk.exception.RateLimitException;
import com.qasimha.sdk.exception.ServerException;
import com.qasimha.sdk.model.OrderCounts;
import com.qasimha.sdk.model.Session;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RetryTest {

  private static final String COUNTS =
      Fixtures.envelope(
          "SUCCESS",
          "{\"all\":1,\"approved\":0,\"authorised\":1,\"captured\":0,\"refunded\":0,\"cancelled\":0}");

  private static final String SESSION =
      Fixtures.envelope(
          "SESSION_CREATED",
          "{\"installmentId\":\"9a2e6c4b-1111-4c2d-8a1f-000000000001\",\"paymentUrl\":\"https://p\",\"status\":\"New\"}");

  private static QasimhaClient client(
      MockApi api, RecordingSleeper sleeper, int maxRetries, Clock clock) {
    QasimhaClient.Builder builder =
        QasimhaClient.builder()
            .apiKey(Fixtures.TEST_KEY)
            .baseUrl(api.baseUrl())
            .maxRetries(maxRetries)
            .sleeper(sleeper);
    if (clock != null) {
      builder.clock(clock);
    }
    return builder.build();
  }

  @Test
  void retriesFiveOhThreeWithExponentialBackoffAndJitter() throws Exception {
    try (MockApi api = new MockApi()) {
      RecordingSleeper sleeper = new RecordingSleeper();
      api.enqueue(503, "").enqueue(503, "").enqueue(200, COUNTS);
      OrderCounts counts = client(api, sleeper, 2, null).orders().counts();
      assertEquals(1, counts.getAll());
      assertEquals(3, api.requestCount());
      List<Long> delays = sleeper.delays();
      assertEquals(2, delays.size());
      assertTrue(delays.get(0) >= 1000 && delays.get(0) <= 1250, delays.toString());
      assertTrue(delays.get(1) >= 2000 && delays.get(1) <= 2250, delays.toString());
    }
  }

  @Test
  void givesUpAfterMaxRetries() throws Exception {
    try (MockApi api = new MockApi()) {
      RecordingSleeper sleeper = new RecordingSleeper();
      api.enqueue(502, "").enqueue(504, "").enqueue(503, "");
      ServerException e =
          assertThrows(
              ServerException.class, () -> client(api, sleeper, 2, null).orders().counts());
      assertEquals(503, e.getHttpStatus());
      assertEquals(3, api.requestCount());
      assertEquals(2, sleeper.delays().size());
    }
  }

  @Test
  void honoursRetryAfterSecondsCappedAtThirty() throws Exception {
    try (MockApi api = new MockApi()) {
      RecordingSleeper sleeper = new RecordingSleeper();
      api.enqueue(429, Map.of("Retry-After", "5"), "")
          .enqueue(429, Map.of("Retry-After", "120"), "")
          .enqueue(200, COUNTS);
      client(api, sleeper, 2, null).orders().counts();
      assertEquals(List.of(5000L, 30000L), sleeper.delays());
    }
  }

  @Test
  void honoursRetryAfterHttpDate() throws Exception {
    try (MockApi api = new MockApi()) {
      RecordingSleeper sleeper = new RecordingSleeper();
      Instant now = Instant.parse("2026-09-25T09:00:00Z");
      Clock clock = Clock.fixed(now, ZoneOffset.UTC);
      String date =
          DateTimeFormatter.RFC_1123_DATE_TIME.format(
              ZonedDateTime.ofInstant(now.plusSeconds(12), ZoneOffset.UTC));
      api.enqueue(429, Map.of("Retry-After", date), "").enqueue(200, COUNTS);
      client(api, sleeper, 1, clock).orders().counts();
      assertEquals(List.of(12000L), sleeper.delays());
    }
  }

  @Test
  void rateLimitAfterRetriesIsRaised() throws Exception {
    try (MockApi api = new MockApi()) {
      RecordingSleeper sleeper = new RecordingSleeper();
      api.enqueue(429, Map.of("Retry-After", "1"), "").enqueue(429, Map.of("Retry-After", "2"), "");
      RateLimitException e =
          assertThrows(
              RateLimitException.class, () -> client(api, sleeper, 1, null).orders().counts());
      assertEquals(2L, e.getRetryAfterSeconds());
      assertEquals(2, api.requestCount());
    }
  }

  @Test
  void requestInProgressIsRetriedThenConflict() throws Exception {
    try (MockApi api = new MockApi()) {
      RecordingSleeper sleeper = new RecordingSleeper();
      String inProgress = "{\"success\":false,\"code\":\"REQUEST_IN_PROGRESS\"}";
      api.enqueue(409, inProgress).enqueue(409, inProgress).enqueue(409, inProgress);
      ConflictException e =
          assertThrows(
              ConflictException.class,
              () ->
                  client(api, sleeper, 2, null)
                      .checkout()
                      .createSession(Fixtures.threeLineBasket("R").build()));
      assertEquals("REQUEST_IN_PROGRESS", e.getCode());
      assertEquals(3, api.requestCount());
      assertEquals(2, sleeper.delays().size());
    }
  }

  @Test
  void requestInProgressThenReplayedResponse() throws Exception {
    try (MockApi api = new MockApi()) {
      RecordingSleeper sleeper = new RecordingSleeper();
      api.enqueue(409, "{\"success\":false,\"code\":\"REQUEST_IN_PROGRESS\"}")
          .enqueue(
              201,
              Map.of("Content-Type", "application/json", "Idempotent-Replay", "true"),
              SESSION);
      Session session =
          client(api, sleeper, 2, null)
              .checkout()
              .createSession(Fixtures.threeLineBasket("R2").build());
      assertTrue(session.isReplayed());
      assertEquals(2, api.requestCount());
    }
  }

  @Test
  void sameIdempotencyKeyAndCorrelationIdAcrossAttempts() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(503, "").enqueue(201, SESSION);
      client(api, new RecordingSleeper(), 1, null)
          .checkout()
          .createSession(Fixtures.threeLineBasket("R3").build());
      List<MockApi.Recorded> requests = api.requests();
      assertEquals(2, requests.size());
      assertNotNull(requests.get(0).header("Idempotency-Key"));
      assertEquals(
          requests.get(0).header("Idempotency-Key"), requests.get(1).header("Idempotency-Key"));
      assertEquals(
          requests.get(0).header("X-Correlation-Id"), requests.get(1).header("X-Correlation-Id"));
      assertEquals(requests.get(0).bodyText(), requests.get(1).bodyText());
    }
  }

  @Test
  void postWithoutIdempotencyIsNeverRetried() throws Exception {
    try (MockApi api = new MockApi()) {
      RecordingSleeper sleeper = new RecordingSleeper();
      api.enqueue(503, "");
      assertThrows(
          ServerException.class,
          () ->
              client(api, sleeper, 3, null)
                  .checkout()
                  .createSession(
                      Fixtures.threeLineBasket("R4").build(), RequestOptions.withoutIdempotency()));
      assertEquals(1, api.requestCount());
      assertTrue(sleeper.delays().isEmpty());
    }
  }

  @Test
  void zeroRetriesMeansOneAttempt() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(503, "");
      assertThrows(
          ServerException.class,
          () -> client(api, new RecordingSleeper(), 0, null).orders().counts());
      assertEquals(1, api.requestCount());
    }
  }
}
