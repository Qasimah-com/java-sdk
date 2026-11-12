package com.qasimha.sdk.internal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * Reads ISO-8601 timestamps with or without an offset; a value without one is UTC, a date-only
 * value is midnight UTC.
 */
public final class LenientInstantDeserializer extends StdDeserializer<Instant> {

  private static final long serialVersionUID = 1L;

  public LenientInstantDeserializer() {
    super(Instant.class);
  }

  @Override
  public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    JsonToken token = parser.currentToken();
    if (token == JsonToken.VALUE_NULL) {
      return null;
    }
    if (token == JsonToken.VALUE_NUMBER_INT) {
      return Instant.ofEpochSecond(parser.getLongValue());
    }
    String text = parser.getValueAsString();
    if (text == null || text.trim().isEmpty()) {
      return null;
    }
    Instant parsed = parse(text.trim());
    if (parsed == null) {
      return (Instant)
          context.handleWeirdStringValue(Instant.class, text, "not an ISO-8601 timestamp");
    }
    return parsed;
  }

  /** Parses a timestamp leniently; returns null when the text is not ISO-8601. */
  public static Instant parse(String text) {
    try {
      return Instant.parse(text);
    } catch (DateTimeParseException ignored) {
      // fall through to the offset, local and date-only forms
    }
    try {
      return OffsetDateTime.parse(text).toInstant();
    } catch (DateTimeParseException ignored) {
      // fall through
    }
    try {
      return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
    } catch (DateTimeParseException ignored) {
      // fall through
    }
    try {
      return LocalDate.parse(text).atStartOfDay(ZoneOffset.UTC).toInstant();
    } catch (DateTimeParseException ignored) {
      return null;
    }
  }
}
