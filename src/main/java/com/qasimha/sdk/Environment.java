package com.qasimha.sdk;

/** The two Qasimha API environments and the key prefixes that select each. */
public enum Environment {
  /** Live traffic, {@code sk_live_} and {@code pk_live_} keys. */
  PRODUCTION("https://api.qasimha.com", "sk_live_", "pk_live_"),
  /** Test traffic, {@code sk_test_} and {@code pk_test_} keys. */
  SANDBOX("https://api-uat.qasimha.com", "sk_test_", "pk_test_");

  private final String baseUrl;
  private final String keyPrefix;
  private final String publishableKeyPrefix;

  Environment(String baseUrl, String keyPrefix, String publishableKeyPrefix) {
    this.baseUrl = baseUrl;
    this.keyPrefix = keyPrefix;
    this.publishableKeyPrefix = publishableKeyPrefix;
  }

  /** The base URL of this environment, without a trailing slash. */
  public String getBaseUrl() {
    return baseUrl;
  }

  /** The API key prefix that belongs to this environment. */
  public String getKeyPrefix() {
    return keyPrefix;
  }

  /** The publishable key prefix that belongs to this environment. */
  public String getPublishableKeyPrefix() {
    return publishableKeyPrefix;
  }

  /**
   * Infers the environment from an API key prefix.
   *
   * @return the environment, or {@code null} when the prefix is neither {@code sk_live_} nor {@code
   *     sk_test_}
   */
  public static Environment forApiKey(String apiKey) {
    if (apiKey == null) {
      return null;
    }
    for (Environment environment : values()) {
      if (apiKey.startsWith(environment.keyPrefix)) {
        return environment;
      }
    }
    return null;
  }

  /**
   * Infers the environment from a publishable key prefix.
   *
   * @return the environment, or {@code null} when the prefix is neither {@code pk_live_} nor {@code
   *     pk_test_}
   */
  public static Environment forPublishableKey(String publishableKey) {
    if (publishableKey == null) {
      return null;
    }
    for (Environment environment : values()) {
      if (publishableKey.startsWith(environment.publishableKeyPrefix)) {
        return environment;
      }
    }
    return null;
  }
}
