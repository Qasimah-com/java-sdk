package com.qasimha.sdk.model;

/**
 * Filters for {@code GET /merchant/webhooks/{id}/deliveries}: an optional {@link WebhookDelivery}
 * status and the page ({@code pageSize} at most 100).
 */
public final class ListDeliveriesParams {

  private final String status;
  private final Integer page;
  private final Integer pageSize;

  private ListDeliveriesParams(Builder builder) {
    this.status = builder.status;
    this.page = builder.page;
    this.pageSize = builder.pageSize;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Every delivery, first page of twenty. */
  public static ListDeliveriesParams all() {
    return builder().build();
  }

  /** The deliveries in one status, first page of twenty. */
  public static ListDeliveriesParams status(String status) {
    return builder().status(status).build();
  }

  public String getStatus() {
    return status;
  }

  public Integer getPage() {
    return page;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  /** Builds {@link ListDeliveriesParams}. */
  public static final class Builder {
    private String status;
    private Integer page;
    private Integer pageSize;

    private Builder() {}

    /** {@code Pending}, {@code Succeeded} or {@code Failed}. */
    public Builder status(String status) {
      this.status = status;
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

    public ListDeliveriesParams build() {
      return new ListDeliveriesParams(this);
    }
  }
}
