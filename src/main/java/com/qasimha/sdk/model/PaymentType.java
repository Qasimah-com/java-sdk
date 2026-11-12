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

/** The payment type of an order, by name; integers and strings are both accepted. */
@JsonDeserialize(using = PaymentType.Deserializer.class)
public final class PaymentType {

  private static final Map<Integer, PaymentType> BY_CODE = new HashMap<>();
  private static final Map<String, PaymentType> BY_NAME = new HashMap<>();

  public static final PaymentType PAY_BY_INSTALMENTS = known(0, "PayByInstalments");
  public static final PaymentType PAY_NOW = known(1, "PayNow");
  public static final PaymentType PAY_NEXT_MONTH = known(2, "PayNextMonth");
  public static final PaymentType PAY_IN_FULL = known(3, "PayInFull");

  private final String name;
  private final Integer code;

  private PaymentType(String name, Integer code) {
    this.name = name;
    this.code = code;
  }

  private static PaymentType known(int code, String name) {
    PaymentType value = new PaymentType(name, code);
    BY_CODE.put(code, value);
    BY_NAME.put(name.toLowerCase(Locale.ROOT), value);
    return value;
  }

  /** The payment type for a name, canonicalising known names and passing unknown ones through. */
  public static PaymentType of(String name) {
    if (name == null) {
      return null;
    }
    PaymentType known = BY_NAME.get(name.toLowerCase(Locale.ROOT));
    if (known != null) {
      return known;
    }
    if (EnumJson.isNumeric(name)) {
      return fromCode(Integer.parseInt(name));
    }
    return new PaymentType(name, null);
  }

  /** The payment type for the API's integer code; an unknown code yields its decimal string. */
  public static PaymentType fromCode(int code) {
    PaymentType known = BY_CODE.get(code);
    return known != null ? known : new PaymentType(Integer.toString(code), null);
  }

  /** The payment type name, for example {@code PayByInstalments}. */
  @JsonValue
  public String getName() {
    return name;
  }

  /** The API's integer code, or null for a value the SDK does not know. */
  public Integer getCode() {
    return code;
  }

  /** Whether this is one of the payment types the SDK knows. */
  public boolean isKnown() {
    return code != null;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof PaymentType && name.equals(((PaymentType) o).name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }

  /** Reads an integer or a string into a {@link PaymentType}. */
  public static final class Deserializer extends StdDeserializer<PaymentType> {

    private static final long serialVersionUID = 1L;

    public Deserializer() {
      super(PaymentType.class);
    }

    @Override
    public PaymentType deserialize(JsonParser parser, DeserializationContext context)
        throws IOException {
      return EnumJson.read(parser, PaymentType::fromCode, PaymentType::of);
    }
  }
}
