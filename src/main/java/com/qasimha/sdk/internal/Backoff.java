package com.qasimha.sdk.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Random;

/**
 * Retry delays: {@code min(2^attempt × 500 ms, 8 s)} plus 0–250 ms of jitter, or {@code
 * Retry-After} (seconds or an HTTP-date) capped at 30 s when the API sent one.
 */
public final class Backoff {

  static final long BASE_MILLIS = 500;
  static final long MAX_MILLIS = 8_000;
  static final long JITTER_MILLIS = 250;
  static final long RETRY_AFTER_CAP_SECONDS = 30;

  private final Random random;
  private final Clock clock;

  public Backoff(Random random, Clock clock) {
    this.random = random;
    this.clock = clock;
  }

  /**
   * The delay before the next attempt.
   *
   * @param failedAttempt the 1-based number of the attempt that just failed
   * @param retryAfter the {@code Retry-After} header of a 429, or null
   */
  public long delayMillis(int failedAttempt, String retryAfter) {
    Long seconds = parseRetryAfterSeconds(retryAfter, clock);
    if (seconds != null) {
      return Math.min(Math.max(seconds, 0), RETRY_AFTER_CAP_SECONDS) * 1_000;
    }
    int exponent = Math.min(Math.max(failedAttempt, 0), 20);
    long base = Math.min(BASE_MILLIS << exponent, MAX_MILLIS);
    return base + random.nextInt((int) JITTER_MILLIS + 1);
  }

  /** Seconds from a {@code Retry-After} value, or null when it is absent or unreadable. */
  public static Long parseRetryAfterSeconds(String value, Clock clock) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    if (trimmed.chars().allMatch(Character::isDigit)) {
      try {
        return Long.parseLong(trimmed);
      } catch (NumberFormatException e) {
        return RETRY_AFTER_CAP_SECONDS;
      }
    }
    try {
      Instant at = ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
      long seconds = Duration.between(clock.instant(), at).getSeconds();
      return Math.max(seconds, 0);
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}
