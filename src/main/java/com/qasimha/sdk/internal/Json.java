package com.qasimha.sdk.internal;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qasimha.sdk.exception.QasimhaException;
import com.qasimha.sdk.model.ResponseMeta;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/** The SDK's JSON mapper: fields only, decimals as {@link java.math.BigDecimal}, nulls omitted. */
public final class Json {

  private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

  private static final ObjectMapper MAPPER =
      JsonMapper.builder()
          .addModule(new JavaTimeModule())
          .addModule(
              new SimpleModule("qasimha-lenient-time")
                  .addDeserializer(Instant.class, new LenientInstantDeserializer()))
          .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
          .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
          .serializationInclusion(JsonInclude.Include.NON_NULL)
          .visibility(PropertyAccessor.FIELD, Visibility.ANY)
          .visibility(PropertyAccessor.GETTER, Visibility.NONE)
          .visibility(PropertyAccessor.IS_GETTER, Visibility.NONE)
          .visibility(PropertyAccessor.SETTER, Visibility.NONE)
          .build();

  private Json() {}

  /** The shared mapper. */
  public static ObjectMapper mapper() {
    return MAPPER;
  }

  /** Serialises a request body as UTF-8 JSON. */
  public static byte[] write(Object body) {
    try {
      return MAPPER.writeValueAsBytes(body);
    } catch (IOException e) {
      throw new QasimhaException("Could not serialise the request body", e);
    }
  }

  /** Parses a body; returns null when it is empty or not JSON. */
  public static JsonNode readTree(byte[] body) {
    if (body == null || body.length == 0) {
      return null;
    }
    try {
      return MAPPER.readTree(body);
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Reads a model, injecting the response metadata into {@link com.qasimha.sdk.model.ApiObject}.
   */
  public static <T> T toModel(JsonNode node, Class<T> type, ResponseMeta meta) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      return null;
    }
    try {
      InjectableValues injectables =
          new InjectableValues.Std().addValue(ResponseMeta.INJECT_ID, meta);
      return MAPPER.readerFor(type).with(injectables).readValue(node);
    } catch (IOException | IllegalArgumentException e) {
      throw new QasimhaException("Could not decode the API response as " + type.getSimpleName(), e);
    }
  }

  /** Reads a plain value object with no metadata. */
  public static <T> T toValue(JsonNode node, Class<T> type) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      return null;
    }
    try {
      return MAPPER.treeToValue(node, type);
    } catch (IOException | IllegalArgumentException e) {
      throw new QasimhaException("Could not decode the API response as " + type.getSimpleName(), e);
    }
  }

  /** Converts an object node to an untyped map; anything else becomes an empty map. */
  public static Map<String, Object> toMap(JsonNode node) {
    if (node == null || !node.isObject()) {
      return Collections.emptyMap();
    }
    return MAPPER.convertValue(node, MAP);
  }

  /** Builds the metadata for a model read from {@code node}. */
  public static ResponseMeta meta(JsonNode node, String requestId, boolean replayed) {
    return new ResponseMeta(toMap(node), requestId, replayed);
  }
}
