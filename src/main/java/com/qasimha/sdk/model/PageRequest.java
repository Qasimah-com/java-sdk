package com.qasimha.sdk.model;

/** A page selector: 1-based {@code page} and {@code pageSize}, both optional. */
public final class PageRequest {

  private static final PageRequest FIRST = new PageRequest(null, null);

  private final Integer page;
  private final Integer pageSize;

  private PageRequest(Integer page, Integer pageSize) {
    this.page = page;
    this.pageSize = pageSize;
  }

  /** The API defaults: page 1 of twenty rows. */
  public static PageRequest first() {
    return FIRST;
  }

  public static PageRequest of(int page, int pageSize) {
    return new PageRequest(page, pageSize);
  }

  public Integer getPage() {
    return page;
  }

  public Integer getPageSize() {
    return pageSize;
  }
}
