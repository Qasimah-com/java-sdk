package com.qasimha.sdk.model;

/**
 * Filters for {@code GET /merchant/orders}. {@code status} takes a status name or one of the
 * aliases {@code approved}, {@code authorised}, {@code captured}, {@code refunded}, {@code
 * cancelled}; {@code search} matches your order reference, the customer name, email or phone.
 */
public final class ListOrdersParams {

  private final String status;
  private final String search;
  private final Integer page;
  private final Integer pageSize;

  private ListOrdersParams(Builder builder) {
    this.status = builder.status;
    this.search = builder.search;
    this.page = builder.page;
    this.pageSize = builder.pageSize;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Every order, first page of twenty. */
  public static ListOrdersParams all() {
    return builder().build();
  }

  /** The orders whose reference, customer name, email or phone match {@code search}. */
  public static ListOrdersParams search(String search) {
    return builder().search(search).build();
  }

  public String getStatus() {
    return status;
  }

  public String getSearch() {
    return search;
  }

  public Integer getPage() {
    return page;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  /** Builds {@link ListOrdersParams}. */
  public static final class Builder {
    private String status;
    private String search;
    private Integer page;
    private Integer pageSize;

    private Builder() {}

    public Builder status(String status) {
      this.status = status;
      return this;
    }

    public Builder status(OrderStatus status) {
      this.status = status == null ? null : status.getName();
      return this;
    }

    public Builder search(String search) {
      this.search = search;
      return this;
    }

    public Builder page(int page) {
      this.page = page;
      return this;
    }

    public Builder pageSize(int pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public ListOrdersParams build() {
      return new ListOrdersParams(this);
    }
  }
}
