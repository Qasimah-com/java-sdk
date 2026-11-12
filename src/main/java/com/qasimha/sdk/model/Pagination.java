package com.qasimha.sdk.model;

/** The paging block of a list response. */
public final class Pagination {

  private long totalItems;
  private int page;
  private int pageSize;
  private int totalPages;

  private Pagination() {}

  public long getTotalItems() {
    return totalItems;
  }

  /** The 1-based page returned. */
  public int getPage() {
    return page;
  }

  public int getPageSize() {
    return pageSize;
  }

  public int getTotalPages() {
    return totalPages;
  }

  /** Whether a page after this one exists. */
  public boolean hasNext() {
    return page < totalPages;
  }
}
