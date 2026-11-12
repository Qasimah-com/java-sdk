package com.qasimha.sdk.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** The channel an order was created through, by name; integers and strings are both accepted. */
@JsonDeserialize(using = OrderChannel.Deserializer.class)
public final class OrderChannel {

  private static final Map<Integer, OrderChannel> BY_CODE = new HashMap<>();
  private static final Map<String, OrderChannel> BY_NAME = new HashMap<>();

  public static final OrderChannel ONLINE = known(0, "Online");
  public static final OrderChannel IN_STORE = known(1, "InStore");
  public static final OrderChannel QR_CODE = known(2, "QrCode");

  private final String name;
  private final Integer code;

  private OrderChannel(String name, Integer code) {
    this.name = name;
    this.code = code;
  }

  private static OrderChannel known(int code, String name) {
    OrderChannel value = new OrderChannel(name, code);
    BY_CODE.put(code, value);
    BY_NAME.put(name.toLowerCase(Locale.ROOT), value);
    return value;
  }

  /** The channel for a name, canonicalising known names and passing unknown ones through. */
  public static OrderChannel of(String name) {
    if (name == null) {
      return null;
    }
    OrderChannel known = BY_NAME.get(name.toLowerCase(Locale.ROOT));
    if (known != null) {
      return known;
    }
    if (EnumJson.isNumeric(name)) {
      return fromCode(Integer.parseInt(name));
    }
    return new OrderChannel(name, null);
  }

  /** The channel for the API's integer code; an unknown code yields its decimal string. */
  public static OrderChannel fromCode(int code) {
    OrderChannel known = BY_CODE.get(code);
    return known != null ? known : new OrderChannel(Integer.toString(code), null);
  }

  /** The channel name, for example {@code Online}. */
  @JsonValue
  public String getName() {
    return name;
  }

  /** The API's integer code, or null for a value the SDK does not know. */
  public Integer getCode() {
    return code;
  }

  /** Whether this is one of the channels the SDK knows. */
  public boolean isKnown() {
    return code != null;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof OrderChannel && name.equals(((OrderChannel) o).name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }

  /** Reads an integer or a string into an {@link OrderChannel}. */
  public static final class Deserializer extends StdDeserializer<OrderChannel> {

    private static final long serialVersionUID = 1L;

    public Deserializer() {
      super(OrderChannel.class);
    }

    @Override
    public OrderChannel deserialize(JsonParser parser, DeserializationContext context)
        throws IOException {
      return EnumJson.read(parser, OrderChannel::fromCode, OrderChannel::of);
    }
  }
}
