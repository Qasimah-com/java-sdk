package com.qasimha.sdk.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The event types a webhook endpoint can subscribe to. {@link #ORDER_COMPLETED} is the name the
 * global webhook used for a confirmed order; the API accepts it as an alias of {@link
 * #ORDER_CONFIRMED} for one release.
 */
public final class WebhookEventTypes {

  public static final String ORDER_CONFIRMED = "order.confirmed";
  public static final String ORDER_DECLINED = "order.declined";
  public static final String ORDER_FULFILLED = "order.fulfilled";
  public static final String ORDER_PARTIALLY_FULFILLED = "order.partially_fulfilled";
  public static final String ORDER_REFUNDED = "order.refunded";
  public static final String ORDER_PARTIALLY_REFUNDED = "order.partially_refunded";
  public static final String ORDER_VOIDED = "order.voided";
  public static final String ORDER_CANCELLED = "order.cancelled";
  public static final String ORDER_EXPIRED = "order.expired";
  public static final String CHECKOUT_LINK_EXPIRED = "checkout_link.expired";

  /** The alias the API still accepts for {@link #ORDER_CONFIRMED}. */
  public static final String ORDER_COMPLETED = "order.completed";

  /** The type of the test event {@code webhookEndpoints().ping(id)} sends; not subscribable. */
  public static final String PING = "ping";

  private static final List<String> KNOWN =
      Collections.unmodifiableList(
          Arrays.asList(
              ORDER_CONFIRMED,
              ORDER_DECLINED,
              ORDER_FULFILLED,
              ORDER_PARTIALLY_FULFILLED,
              ORDER_REFUNDED,
              ORDER_PARTIALLY_REFUNDED,
              ORDER_VOIDED,
              ORDER_CANCELLED,
              ORDER_EXPIRED,
              CHECKOUT_LINK_EXPIRED));

  private WebhookEventTypes() {}

  /** Every subscribable event type, without the alias. */
  public static List<String> known() {
    return KNOWN;
  }

  /**
   * Whether {@code name} is a subscribable type or the alias, ignoring case and surrounding space.
   */
  public static boolean isKnown(String name) {
    if (name == null) {
      return false;
    }
    String trimmed = name.trim().toLowerCase(Locale.ROOT);
    return trimmed.equals(ORDER_COMPLETED) || KNOWN.contains(trimmed);
  }
}
