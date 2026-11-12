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

/** The status of one scheduled instalment, by name; integers and strings are both accepted. */
@JsonDeserialize(using = InstallmentItemStatus.Deserializer.class)
public final class InstallmentItemStatus {

  private static final Map<Integer, InstallmentItemStatus> BY_CODE = new HashMap<>();
  private static final Map<String, InstallmentItemStatus> BY_NAME = new HashMap<>();

  public static final InstallmentItemStatus SCHEDULED = known(0, "Scheduled");
  public static final InstallmentItemStatus PAID = known(1, "Paid");
  public static final InstallmentItemStatus LATE = known(2, "Late");
  public static final InstallmentItemStatus REVERSED = known(3, "Reversed");
  public static final InstallmentItemStatus WAIVED = known(4, "Waived");

  private final String name;
  private final Integer code;

  private InstallmentItemStatus(String name, Integer code) {
    this.name = name;
    this.code = code;
  }

  private static InstallmentItemStatus known(int code, String name) {
    InstallmentItemStatus value = new InstallmentItemStatus(name, code);
    BY_CODE.put(code, value);
    BY_NAME.put(name.toLowerCase(Locale.ROOT), value);
    return value;
  }

  /** The status for a name, canonicalising known names and passing unknown ones through. */
  public static InstallmentItemStatus of(String name) {
    if (name == null) {
      return null;
    }
    InstallmentItemStatus known = BY_NAME.get(name.toLowerCase(Locale.ROOT));
    if (known != null) {
      return known;
    }
    if (EnumJson.isNumeric(name)) {
      return fromCode(Integer.parseInt(name));
    }
    return new InstallmentItemStatus(name, null);
  }

  /** The status for the API's integer code; an unknown code yields its decimal string. */
  public static InstallmentItemStatus fromCode(int code) {
    InstallmentItemStatus known = BY_CODE.get(code);
    return known != null ? known : new InstallmentItemStatus(Integer.toString(code), null);
  }

  /** The status name, for example {@code Paid}. */
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
    return o instanceof InstallmentItemStatus && name.equals(((InstallmentItemStatus) o).name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }

  /** Reads an integer or a string into an {@link InstallmentItemStatus}. */
  public static final class Deserializer extends StdDeserializer<InstallmentItemStatus> {

    private static final long serialVersionUID = 1L;

    public Deserializer() {
      super(InstallmentItemStatus.class);
    }

    @Override
    public InstallmentItemStatus deserialize(JsonParser parser, DeserializationContext context)
        throws IOException {
      return EnumJson.read(parser, InstallmentItemStatus::fromCode, InstallmentItemStatus::of);
    }
  }
}
