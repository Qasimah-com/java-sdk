package com.qasimha.sdk.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.util.function.Function;
import java.util.function.IntFunction;

final class EnumJson {

  private EnumJson() {}

  static <T> T read(JsonParser parser, IntFunction<T> byCode, Function<String, T> byName)
      throws IOException {
    JsonToken token = parser.currentToken();
    if (token == JsonToken.VALUE_NUMBER_INT) {
      return byCode.apply(parser.getIntValue());
    }
    if (token == JsonToken.VALUE_STRING) {
      return byName.apply(parser.getText());
    }
    if (token == JsonToken.VALUE_NULL) {
      return null;
    }
    return byName.apply(parser.getValueAsString());
  }

  static boolean isNumeric(String value) {
    if (value == null || value.isEmpty() || value.length() > 9) {
      return false;
    }
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    return true;
  }
}
