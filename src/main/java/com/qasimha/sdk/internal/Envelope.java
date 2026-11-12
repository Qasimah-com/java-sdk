package com.qasimha.sdk.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.qasimha.sdk.model.FieldError;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** The decoded response envelope: {@code success, uuid, code, message, data, errors}. */
public final class Envelope {

  private final Boolean success;
  private final String uuid;
  private final String code;
  private final String message;
  private final JsonNode data;
  private final List<FieldError> errors;
  private final Map<String, Object> raw;

  private Envelope(JsonNode root) {
    JsonNode successNode = root.has("success") ? root.get("success") : root.get("isSuccess");
    this.success =
        successNode != null && successNode.isBoolean() ? successNode.booleanValue() : null;
    this.uuid = text(root, "uuid");
    this.code = text(root, "code");
    this.message = text(root, "message");
    this.data = root.get("data");
    this.errors = readErrors(root.get("errors"));
    this.raw = Json.toMap(root);
  }

  /** Parses an envelope; returns null when the body is not a JSON object. */
  public static Envelope parse(JsonNode root) {
    if (root == null || !root.isObject()) {
      return null;
    }
    return new Envelope(root);
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value == null || value.isNull() ? null : value.asText();
  }

  private static List<FieldError> readErrors(JsonNode node) {
    if (node == null || !node.isArray() || node.isEmpty()) {
      return Collections.emptyList();
    }
    List<FieldError> list = new ArrayList<>(node.size());
    for (JsonNode entry : node) {
      if (entry.isObject()) {
        list.add(new FieldError(text(entry, "field"), text(entry, "message")));
      } else if (entry.isTextual()) {
        list.add(new FieldError(null, entry.textValue()));
      }
    }
    return Collections.unmodifiableList(list);
  }

  /** The envelope {@code success} flag, or null when absent. */
  public Boolean getSuccess() {
    return success;
  }

  /** Whether the envelope explicitly says {@code success: false}. */
  public boolean isFailure() {
    return Boolean.FALSE.equals(success);
  }

  public String getUuid() {
    return uuid;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  /** The {@code data} node, or null. */
  public JsonNode getData() {
    return data;
  }

  /** A text member of {@code data}, or null. */
  public String dataText(String field) {
    if (data == null || !data.isObject()) {
      return null;
    }
    return text(data, field);
  }

  public List<FieldError> getErrors() {
    return errors;
  }

  public Map<String, Object> getRaw() {
    return raw;
  }
}
