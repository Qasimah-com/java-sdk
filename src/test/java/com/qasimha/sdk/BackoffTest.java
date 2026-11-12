package com.qasimha.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qasimha.sdk.internal.Backoff;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import org.junit.jupiter.api.Test;

class BackoffTest {

  private static final Instant NOW = Instant.parse("2026-09-25T09:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  private static final Random NO_JITTER =
      new Random() {
        private static final long serialVersionUID = 1L;

        @Override
        public int nextInt(int bound) {
          return 0;
        }
      };

  private static final Random MAX_JITTER =
      new Random() {
        private static final long serialVersionUID = 1L;

        @Override
        public int nextInt(int bound) {
          return bound - 1;
        }
      };

  @Test
  void exponentialCappedAtEightSeconds() {
    Backoff backoff = new Backoff(NO_JITTER, CLOCK);
    assertEquals(1000, backoff.delayMillis(1, null));
    assertEquals(2000, backoff.delayMillis(2, null));
    assertEquals(4000, backoff.delayMillis(3, null));
    assertEquals(8000, backoff.delayMillis(4, null));
    assertEquals(8000, backoff.delayMillis(5, null));
    assertEquals(8000, backoff.delayMillis(50, null));
  }

  @Test
  void jitterIsAtMostTwoHundredFiftyMillis() {
    assertEquals(1250, new Backoff(MAX_JITTER, CLOCK).delayMillis(1, null));
    long sampled = new Backoff(new Random(), CLOCK).delayMillis(1, null);
    assertTrue(sampled >= 1000 && sampled <= 1250, String.valueOf(sampled));
  }

  @Test
  void retryAfterSecondsWinsAndIsCapped() {
    Backoff backoff = new Backoff(MAX_JITTER, CLOCK);
    assertEquals(7000, backoff.delayMillis(1, "7"));
    assertEquals(0, backoff.delayMillis(1, "0"));
    assertEquals(30000, backoff.delayMillis(1, "120"));
    assertEquals(30000, backoff.delayMillis(1, "99999999999999999999"));
  }

  @Test
  void retryAfterHttpDate() {
    Backoff backoff = new Backoff(NO_JITTER, CLOCK);
    String future =
        DateTimeFormatter.RFC_1123_DATE_TIME.format(
            ZonedDateTime.ofInstant(NOW.plusSeconds(10), ZoneOffset.UTC));
    String past =
        DateTimeFormatter.RFC_1123_DATE_TIME.format(
            ZonedDateTime.ofInstant(NOW.minusSeconds(10), ZoneOffset.UTC));
    assertEquals(10000, backoff.delayMillis(1, future));
    assertEquals(0, backoff.delayMillis(1, past));
    assertEquals(10L, Backoff.parseRetryAfterSeconds(future, CLOCK));
  }

  @Test
  void unreadableRetryAfterFallsBackToExponential() {
    Backoff backoff = new Backoff(NO_JITTER, CLOCK);
    assertEquals(2000, backoff.delayMillis(2, "soon"));
    assertEquals(2000, backoff.delayMillis(2, "  "));
    assertNull(Backoff.parseRetryAfterSeconds(null, CLOCK));
    assertNull(Backoff.parseRetryAfterSeconds("soon", CLOCK));
  }
}
