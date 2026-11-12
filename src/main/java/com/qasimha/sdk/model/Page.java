package com.qasimha.sdk.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** One page of a list response. */
public final class Page<T> {

  private final List<T> items;
  private final Pagination pagination;
  private final ResponseMeta meta;

  public Page(List<T> items, Pagination pagination, ResponseMeta meta) {
    this.items = Collections.unmodifiableList(new ArrayList<>(items));
    this.pagination = pagination;
    this.meta = meta == null ? ResponseMeta.NONE : meta;
  }

  public List<T> getItems() {
    return items;
  }

  public Pagination getPagination() {
    return pagination;
  }

  /** The untyped decoded {@code data} of the response. */
  public Map<String, Object> getRaw() {
    return meta.getRaw();
  }

  /** The request id (envelope {@code uuid}, else the {@code X-Request-Id} header). */
  public String getRequestId() {
    return meta.getRequestId();
  }

  /** Whether the response carried {@code Idempotent-Replay: true}. */
  public boolean isReplayed() {
    return meta.isReplayed();
  }
}
