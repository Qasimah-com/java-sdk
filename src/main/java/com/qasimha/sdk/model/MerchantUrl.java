package com.qasimha.sdk.model;

/**
 * Where the hosted checkout sends the shopper afterwards; honoured since the API release of
 * 2026-09-25.
 */
public final class MerchantUrl {

  private String success;
  private String failure;
  private String cancel;
  private String notification;

  private MerchantUrl() {}

  private MerchantUrl(Builder builder) {
    this.success = builder.success;
    this.failure = builder.failure;
    this.cancel = builder.cancel;
    this.notification = builder.notification;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getSuccess() {
    return success;
  }

  public String getFailure() {
    return failure;
  }

  public String getCancel() {
    return cancel;
  }

  public String getNotification() {
    return notification;
  }

  /** Builds a {@link MerchantUrl}. */
  public static final class Builder {
    private String success;
    private String failure;
    private String cancel;
    private String notification;

    private Builder() {}

    public Builder success(String success) {
      this.success = success;
      return this;
    }

    public Builder failure(String failure) {
      this.failure = failure;
      return this;
    }

    public Builder cancel(String cancel) {
      this.cancel = cancel;
      return this;
    }

    public Builder notification(String notification) {
      this.notification = notification;
      return this;
    }

    public MerchantUrl build() {
      return new MerchantUrl(this);
    }
  }
}
