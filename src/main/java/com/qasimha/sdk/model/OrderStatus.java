package com.qasimha.sdk.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The order status by name. The API serialises it as an integer today and as the name in the
 * documented contract; both are accepted and a value the SDK does not know is passed through as
 * received, so a new status never breaks an installed integration.
 */
@JsonDeserialize(using = OrderStatus.Deserializer.class)
public final class OrderStatus {

  private static final Map<Integer, OrderStatus> BY_CODE = new HashMap<>();
  private static final Map<String, OrderStatus> BY_NAME = new LinkedHashMap<>();

  public static final OrderStatus DRAFT = known(0, "Draft");
  public static final OrderStatus NEW = known(1, "New");
  public static final OrderStatus APPROVED = known(2, "Approved");
  public static final OrderStatus DECLINED = known(3, "Declined");
  public static final OrderStatus EXPIRED = known(4, "Expired");
  public static final OrderStatus CONFIRMED = known(5, "Confirmed");
  public static final OrderStatus FULLY_FULFILLED = known(6, "FullyFulfilled");
  public static final OrderStatus PARTIALLY_FULFILLED = known(7, "PartiallyFulfilled");
  public static final OrderStatus FULLY_REVERSED = known(8, "FullyReversed");
  public static final OrderStatus PARTIALLY_REVERSED = known(9, "PartiallyReversed");
  public static final OrderStatus VOIDED = known(10, "Voided");
  public static final OrderStatus UPDATED = known(11, "Updated");
  public static final OrderStatus AWAITING_CHECKOUT = known(12, "AwaitingCheckout");
  public static final OrderStatus CANCELLED = known(13, "Cancelled");
  public static final OrderStatus SETTLED = known(14, "Settled");
  public static final OrderStatus WRITTEN_OFF = known(15, "WrittenOff");

  private final String name;
  private final Integer code;

  private OrderStatus(String name, Integer code) {
    this.name = name;
    this.code = code;
  }

  private static OrderStatus known(int code, String name) {
    OrderStatus value = new OrderStatus(name, code);
    BY_CODE.put(code, value);
    BY_NAME.put(name.toLowerCase(Locale.ROOT), value);
    return value;
  }

  /** The status for a name, canonicalising known names and passing unknown ones through. */
  public static OrderStatus of(String name) {
    if (name == null) {
      return null;
    }
    OrderStatus known = BY_NAME.get(name.toLowerCase(Locale.ROOT));
    if (known != null) {
      return known;
    }
    if (EnumJson.isNumeric(name)) {
      return fromCode(Integer.parseInt(name));
    }
    return new OrderStatus(name, null);
  }

  /** The status for the API's integer code; an unknown code yields its decimal string. */
  public static OrderStatus fromCode(int code) {
    OrderStatus known = BY_CODE.get(code);
    return known != null ? known : new OrderStatus(Integer.toString(code), null);
  }

  /** Every status the SDK knows, in code order. */
  public static List<OrderStatus> known() {
    return Collections.unmodifiableList(new ArrayList<>(BY_NAME.values()));
  }

  /** The status name, for example {@code Confirmed}. */
  @JsonValue
  public String getName() {
    return name;
  }

  /** The API's integer code, or null for a value the SDK does not know. */
  public Integer getCode() {
    return code;
  }

  /** Whether this is one of the statuses the SDK knows. */
  public boolean isKnown() {
    return code != null;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof OrderStatus && name.equals(((OrderStatus) o).name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }

  /** Reads an integer or a string into an {@link OrderStatus}. */
  public static final class Deserializer extends StdDeserializer<OrderStatus> {

    private static final long serialVersionUID = 1L;

    public Deserializer() {
      super(OrderStatus.class);
    }

    @Override
    public OrderStatus deserialize(JsonParser parser, DeserializationContext context)
        throws IOException {
      return EnumJson.read(parser, OrderStatus::fromCode, OrderStatus::of);
    }
  }
}
