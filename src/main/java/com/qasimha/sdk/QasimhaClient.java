package com.qasimha.sdk;

import com.qasimha.sdk.exception.ConfigurationException;
import com.qasimha.sdk.internal.Backoff;
import com.qasimha.sdk.internal.HttpTransport;
import com.qasimha.sdk.internal.UserAgent;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * The entry point: one client per API key, safe to share between threads.
 *
 * <pre>{@code
 * QasimhaClient client = QasimhaClient.builder()
 *     .apiKey(System.getenv("QASIMHA_API_KEY"))
 *     .webhookSecret(System.getenv("QASIMHA_WEBHOOK_SECRET"))
 *     .publishableKey(System.getenv("QASIMHA_PUBLISHABLE_KEY"))
 *     .build();
 * Session session = client.checkout().createSession(request);
 * }</pre>
 */
public final class QasimhaClient {

  private static final Pattern API_KEY = Pattern.compile("^sk_(test|live)_[A-Za-z0-9]+$");
  static final Pattern PUBLISHABLE_KEY = Pattern.compile("^pk_(test|live)_[A-Za-z0-9]+$");
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
  private static final int DEFAULT_MAX_RETRIES = 2;
  private static final int MAX_RETRIES_LIMIT = 5;

  private final Environment environment;
  private final String baseUrl;
  private final String apiKey;
  private final String publishableKey;
  private final String userAgent;
  private final Checkout checkout;
  private final Orders orders;
  private final CheckoutLinks checkoutLinks;
  private final Settlements settlements;
  private final Webhooks webhooks;
  private final WebhookEndpoints webhookEndpoints;

  private QasimhaClient(Builder builder) {
    this.environment = builder.resolvedEnvironment;
    this.baseUrl = builder.resolvedBaseUrl;
    this.apiKey = builder.apiKey;
    this.publishableKey = builder.publishableKey;
    this.userAgent = UserAgent.build(builder.userAgentSuffix);
    HttpClient httpClient =
        builder.httpClient != null
            ? builder.httpClient
            : HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(builder.timeout)
                .build();
    Clock clock = builder.clock != null ? builder.clock : Clock.systemUTC();
    HttpTransport transport =
        new HttpTransport(
            httpClient,
            baseUrl,
            apiKey,
            publishableKey,
            builder.timeout,
            builder.maxRetries,
            builder.language,
            userAgent,
            builder.logger,
            builder.sleeper != null ? builder.sleeper : Sleeper.system(),
            clock,
            new Backoff(new Random(), clock));
    this.checkout = new Checkout(transport, Environment.forApiKey(apiKey));
    this.orders = new Orders(transport);
    this.checkoutLinks = new CheckoutLinks(transport);
    this.settlements = new Settlements(transport);
    this.webhooks = new Webhooks(builder.webhookSecret, clock);
    this.webhookEndpoints = new WebhookEndpoints(transport);
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Online, POS and QR sessions, and plan quotes for an amount. */
  public Checkout checkout() {
    return checkout;
  }

  /** Orders: list, get, getByReference, capture, cancel, refund, fulfil, counts, contract. */
  public Orders orders() {
    return orders;
  }

  /** Payment links and QR orders. */
  public CheckoutLinks checkoutLinks() {
    return checkoutLinks;
  }

  /** Settlement batches and overview. */
  public Settlements settlements() {
    return settlements;
  }

  /** Webhook verification with the configured {@code webhookSecret}. */
  public Webhooks webhooks() {
    return webhooks;
  }

  /** Webhook endpoints: create, list, get, update, delete, rotate secret, ping, deliveries. */
  public WebhookEndpoints webhookEndpoints() {
    return webhookEndpoints;
  }

  /** The environment inferred from the key or given explicitly. */
  public Environment getEnvironment() {
    return environment;
  }

  /** The base URL requests are sent to, without a trailing slash. */
  public String getBaseUrl() {
    return baseUrl;
  }

  /** The {@code User-Agent} header this client sends. */
  public String getUserAgent() {
    return userAgent;
  }

  /** The key with its middle hidden: {@code sk_live_…AbCd}. */
  public String maskedKey() {
    return maskKey(apiKey);
  }

  /** The publishable key with its middle hidden ({@code pk_live_…AbCd}), or null when not set. */
  public String maskedPublishableKey() {
    return publishableKey == null ? null : maskKey(publishableKey);
  }

  static String maskKey(String key) {
    String prefix = keyPrefix(key);
    String body = key.substring(prefix.length());
    String tail = body.length() <= 4 ? body : body.substring(body.length() - 4);
    return prefix + "…" + tail;
  }

  private static String keyPrefix(String key) {
    Environment secret = Environment.forApiKey(key);
    if (secret != null) {
      return secret.getKeyPrefix();
    }
    Environment publishable = Environment.forPublishableKey(key);
    return publishable == null ? "" : publishable.getPublishableKeyPrefix();
  }

  @Override
  public String toString() {
    StringBuilder sb =
        new StringBuilder("QasimhaClient{environment=")
            .append(environment)
            .append(", baseUrl=")
            .append(baseUrl)
            .append(", apiKey=")
            .append(maskedKey());
    if (publishableKey != null) {
      sb.append(", publishableKey=").append(maskedPublishableKey());
    }
    return sb.append('}').toString();
  }

  /** Builds a {@link QasimhaClient}; every rule violation is a {@link ConfigurationException}. */
  public static final class Builder {
    private String apiKey;
    private Environment environment;
    private String baseUrl;
    private Duration timeout = DEFAULT_TIMEOUT;
    private int maxRetries = DEFAULT_MAX_RETRIES;
    private String webhookSecret;
    private String publishableKey;
    private String language = "en";
    private String userAgentSuffix;
    private Logger logger;
    private HttpClient httpClient;
    private Sleeper sleeper;
    private Clock clock;
    private Environment resolvedEnvironment;
    private String resolvedBaseUrl;

    private Builder() {}

    /** Required: an {@code sk_test_...} or {@code sk_live_...} key. */
    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    /** Optional; inferred from the key prefix and must not contradict it. */
    public Builder environment(Environment environment) {
      this.environment = environment;
      return this;
    }

    /** Optional override; wins over the environment and must be {@code https://}. */
    public Builder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    /** Per-attempt timeout, default 30 s. */
    public Builder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    /** Retries after the first attempt, 0 to 5, default 2. */
    public Builder maxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    /** The webhook signing secret used by {@link QasimhaClient#webhooks()}. */
    public Builder webhookSecret(String webhookSecret) {
      this.webhookSecret = webhookSecret;
      return this;
    }

    /**
     * Optional: a {@code pk_test_...} or {@code pk_live_...} key for {@link
     * Checkout#getPlans(java.math.BigDecimal)}. Its environment must be the API key's.
     */
    public Builder publishableKey(String publishableKey) {
      this.publishableKey = publishableKey;
      return this;
    }

    /** {@code ar} or {@code en} for localised API messages, default {@code en}. */
    public Builder language(String language) {
      this.language = language;
      return this;
    }

    /** Appended to the {@code User-Agent}, for example {@code woocommerce/1.2.0}. */
    public Builder userAgentSuffix(String userAgentSuffix) {
      this.userAgentSuffix = userAgentSuffix;
      return this;
    }

    /** Optional log callback; nothing is logged without it. */
    public Builder logger(Logger logger) {
      this.logger = logger;
      return this;
    }

    /**
     * An {@link HttpClient} to send with instead of the default one. Build it with {@link
     * HttpClient.Redirect#NEVER}; the SDK treats any 3xx as an error.
     */
    public Builder httpClient(HttpClient httpClient) {
      this.httpClient = httpClient;
      return this;
    }

    /** Replaces the pause between retries; for tests. */
    public Builder sleeper(Sleeper sleeper) {
      this.sleeper = sleeper;
      return this;
    }

    /** Replaces the clock used for {@code Retry-After} dates and webhook tolerance; for tests. */
    public Builder clock(Clock clock) {
      this.clock = clock;
      return this;
    }

    public QasimhaClient build() {
      if (apiKey == null || !API_KEY.matcher(apiKey).matches()) {
        throw new ConfigurationException(
            "apiKey is required and must look like sk_test_... or sk_live_...");
      }
      Environment inferred = Environment.forApiKey(apiKey);
      if (baseUrl != null) {
        resolvedBaseUrl = validateBaseUrl(baseUrl);
        resolvedEnvironment = environment != null ? environment : inferred;
      } else {
        if (environment != null && environment != inferred) {
          throw new ConfigurationException(
              "environment "
                  + environment
                  + " contradicts the key prefix "
                  + inferred.getKeyPrefix());
        }
        resolvedEnvironment = inferred;
        resolvedBaseUrl = inferred.getBaseUrl();
      }
      if (timeout == null || timeout.isZero() || timeout.isNegative()) {
        throw new ConfigurationException("timeout must be greater than zero");
      }
      if (maxRetries < 0 || maxRetries > MAX_RETRIES_LIMIT) {
        throw new ConfigurationException("maxRetries must be between 0 and " + MAX_RETRIES_LIMIT);
      }
      if (language == null || !(language.equals("ar") || language.equals("en"))) {
        throw new ConfigurationException("language must be ar or en");
      }
      if (userAgentSuffix != null && !UserAgent.isHeaderSafe(userAgentSuffix)) {
        throw new ConfigurationException("userAgentSuffix must be printable ASCII");
      }
      if (webhookSecret != null && webhookSecret.trim().isEmpty()) {
        throw new ConfigurationException("webhookSecret must not be blank when given");
      }
      if (publishableKey != null) {
        requirePublishableKey(publishableKey, inferred);
      }
      return new QasimhaClient(this);
    }

    static void requirePublishableKey(String publishableKey, Environment secretKeyEnvironment) {
      if (!PUBLISHABLE_KEY.matcher(publishableKey).matches()) {
        throw new ConfigurationException(
            "publishableKey must look like pk_test_... or pk_live_...");
      }
      Environment publishable = Environment.forPublishableKey(publishableKey);
      if (publishable != secretKeyEnvironment) {
        throw new ConfigurationException(
            "publishableKey prefix "
                + publishable.getPublishableKeyPrefix()
                + " does not match the apiKey prefix "
                + secretKeyEnvironment.getKeyPrefix());
      }
    }

    private static String validateBaseUrl(String value) {
      URI uri;
      try {
        uri = new URI(value.trim());
      } catch (java.net.URISyntaxException e) {
        throw new ConfigurationException("baseUrl is not a valid URL");
      }
      String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
      String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
      if (host.isEmpty()) {
        throw new ConfigurationException("baseUrl must include a host");
      }
      boolean loopback = host.equals("localhost") || host.equals("127.0.0.1");
      if (!scheme.equals("https") && !(scheme.equals("http") && loopback)) {
        throw new ConfigurationException(
            "baseUrl must use https:// (http:// is allowed for localhost and 127.0.0.1 only)");
      }
      if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
        throw new ConfigurationException("baseUrl must not carry a query or fragment");
      }
      String normalised = uri.toString();
      while (normalised.endsWith("/")) {
        normalised = normalised.substring(0, normalised.length() - 1);
      }
      return normalised;
    }
  }
}
