package com.qasimha.sdk;

import com.qasimha.sdk.exception.ConfigurationException;
import java.time.Duration;
import java.time.Instant;

/** Verification options: the replay tolerance (default 300 s) and a fixed {@code now} for tests. */
public final class WebhookOptions {

  /** The default replay tolerance. */
  public static final Duration DEFAULT_TOLERANCE = Duration.ofSeconds(300);

  private static final WebhookOptions DEFAULTS = new WebhookOptions(DEFAULT_TOLERANCE, null);

  private final Duration tolerance;
  private final Instant now;

  private WebhookOptions(Duration tolerance, Instant now) {
    this.tolerance = tolerance;
    this.now = now;
  }

  public static WebhookOptions defaults() {
    return DEFAULTS;
  }

  /** The same options with another tolerance. */
  public WebhookOptions tolerance(Duration tolerance) {
    if (tolerance == null || tolerance.isNegative()) {
      throw new ConfigurationException("tolerance must be zero or positive");
    }
    return new WebhookOptions(tolerance, now);
  }

  /** The same options with a fixed current time. */
  public WebhookOptions now(Instant now) {
    return new WebhookOptions(tolerance, now);
  }

  public Duration getTolerance() {
    return tolerance;
  }

  /** The fixed current time, or null to use the clock. */
  public Instant getNow() {
    return now;
  }
}
