package com.qasimha.sdk;

import com.qasimha.sdk.exception.ConfigurationException;

/**
 * Per-call options for {@link Checkout#getPlans(java.math.BigDecimal, GetPlansOptions)}: the quote
 * language ({@code ar} or {@code en}, default the client's {@code language}) and a publishable key
 * that replaces the client's for this call.
 */
public final class GetPlansOptions {

  private static final GetPlansOptions DEFAULTS = new GetPlansOptions(null, null);

  private final String language;
  private final String publishableKey;

  private GetPlansOptions(String language, String publishableKey) {
    this.language = language;
    this.publishableKey = publishableKey;
  }

  /** The client's language and publishable key. */
  public static GetPlansOptions defaults() {
    return DEFAULTS;
  }

  /** The same options quoting in {@code ar} or {@code en}. */
  public GetPlansOptions language(String language) {
    if (language == null || !(language.equals("ar") || language.equals("en"))) {
      throw new ConfigurationException("language must be ar or en");
    }
    return new GetPlansOptions(language, publishableKey);
  }

  /**
   * The same options authenticating with another {@code pk_test_...} or {@code pk_live_...} key.
   */
  public GetPlansOptions publishableKey(String publishableKey) {
    if (publishableKey == null
        || !QasimhaClient.PUBLISHABLE_KEY.matcher(publishableKey).matches()) {
      throw new ConfigurationException("publishableKey must look like pk_test_... or pk_live_...");
    }
    return new GetPlansOptions(language, publishableKey);
  }

  /** The quote language, or null to use the client's. */
  public String getLanguage() {
    return language;
  }

  /** The publishable key for this call, or null to use the client's. */
  public String getPublishableKey() {
    return publishableKey;
  }
}
