package com.qasimha.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.qasimha.sdk.exception.ConfigurationException;
import com.qasimha.sdk.internal.Envelope;
import com.qasimha.sdk.internal.ErrorMapper;
import com.qasimha.sdk.internal.HttpResult;
import com.qasimha.sdk.internal.HttpTransport;
import com.qasimha.sdk.internal.Json;
import com.qasimha.sdk.model.Page;
import com.qasimha.sdk.model.Pagination;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ResourceSupport {

  private ResourceSupport() {}

  static void ensureSuccess(HttpTransport transport, HttpResult result) {
    Envelope envelope = result.getEnvelope();
    if (!result.isSuccess() || (envelope != null && envelope.isFailure())) {
      throw ErrorMapper.map(result, transport.clock());
    }
  }

  static JsonNode data(HttpResult result) {
    Envelope envelope = result.getEnvelope();
    return envelope == null ? null : envelope.getData();
  }

  static <T> T readData(HttpResult result, Class<T> type) {
    JsonNode data = data(result);
    return Json.toModel(data, type, Json.meta(data, result.getRequestId(), result.isReplayed()));
  }

  static <T> List<T> readList(JsonNode array, Class<T> type, HttpResult result) {
    if (array == null || !array.isArray()) {
      return Collections.emptyList();
    }
    List<T> items = new ArrayList<>(array.size());
    for (JsonNode node : array) {
      items.add(
          Json.toModel(node, type, Json.meta(node, result.getRequestId(), result.isReplayed())));
    }
    return Collections.unmodifiableList(items);
  }

  static <T> Page<T> readPage(HttpResult result, Class<T> type) {
    JsonNode data = data(result);
    JsonNode rows = null;
    JsonNode pagination = null;
    if (data != null && data.isObject()) {
      rows = data.has("data") ? data.get("data") : data.get("items");
      pagination = data.get("pagination");
    }
    return new Page<>(
        readList(rows, type, result),
        Json.toValue(pagination, Pagination.class),
        Json.meta(data, result.getRequestId(), result.isReplayed()));
  }

  static String requireId(String id, String name) {
    if (id == null || id.trim().isEmpty()) {
      throw new ConfigurationException(name + " must not be blank");
    }
    return HttpTransport.segment(id.trim());
  }

  static <T> T requireNonNull(T value, String name) {
    if (value == null) {
      throw new ConfigurationException(name + " must not be null");
    }
    return value;
  }
}
