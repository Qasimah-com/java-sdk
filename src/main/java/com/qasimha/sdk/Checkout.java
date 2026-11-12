package com.qasimha.sdk;

import com.qasimha.sdk.exception.ConfigurationException;
import com.qasimha.sdk.exception.OrderDeclinedException;
import com.qasimha.sdk.exception.ValidationException;
import com.qasimha.sdk.internal.BasketValidator;
import com.qasimha.sdk.internal.ErrorMapper;
import com.qasimha.sdk.internal.HttpResult;
import com.qasimha.sdk.internal.HttpTransport;
import com.qasimha.sdk.internal.Money;
import com.qasimha.sdk.model.CreatePosSessionRequest;
import com.qasimha.sdk.model.CreateQrSessionRequest;
import com.qasimha.sdk.model.CreateSessionRequest;
import com.qasimha.sdk.model.FieldError;
import com.qasimha.sdk.model.OrderStatus;
import com.qasimha.sdk.model.Plans;
import com.qasimha.sdk.model.PosSession;
import com.qasimha.sdk.model.QrSession;
import com.qasimha.sdk.model.Session;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Starts an order (online session, in-store (POS) session or scan-to-pay QR session) and quotes the
 * instalment plans for an amount before one is started.
 */
public final class Checkout {

  private final HttpTransport transport;
  private final Environment environment;

  Checkout(HttpTransport transport, Environment environment) {
    this.transport = transport;
    this.environment = environment;
  }

  /** Creates an online checkout session and returns the URL to redirect the shopper to. */
  public Session createSession(CreateSessionRequest request) {
    return createSession(request, RequestOptions.defaults());
  }

  /**
   * Creates an online checkout session.
   *
   * @throws com.qasimha.sdk.exception.ValidationException when the basket fails the client-side
   *     checks, or the API answers 400
   * @throws com.qasimha.sdk.exception.DuplicateOrderReferenceException when the reference is in use
   * @throws OrderDeclinedException when the order was created but not approved
   */
  public Session createSession(CreateSessionRequest request, RequestOptions options) {
    ResourceSupport.requireNonNull(request, "request");
    BasketValidator.validate(request);
    HttpResult result = transport.post("/pay/session/create", request, options);
    if (isDuplicateOnRetry(result)) {
      logRecoveredDuplicate(result);
      return ResourceSupport.readData(result, Session.class);
    }
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, Session.class);
  }

  /** Creates an in-store order and returns the hosted checkout page for the customer. */
  public PosSession createPosSession(CreatePosSessionRequest request) {
    return createPosSession(request, RequestOptions.defaults());
  }

  /** Creates an in-store order; {@code consumer.email} is required. */
  public PosSession createPosSession(CreatePosSessionRequest request, RequestOptions options) {
    ResourceSupport.requireNonNull(request, "request");
    BasketValidator.validate(request);
    HttpResult result = transport.post("/pay/pos/create", request, options);
    if (isDuplicateOnRetry(result)) {
      logRecoveredDuplicate(result);
      return ResourceSupport.readData(result, PosSession.class);
    }
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, PosSession.class);
  }

  /** Creates a scan-to-pay QR session. */
  public QrSession createQrSession(CreateQrSessionRequest request) {
    return createQrSession(request, RequestOptions.defaults());
  }

  /**
   * Creates a scan-to-pay QR session. A declined order, which the API answers with 201 and an empty
   * {@code qrCodeUrl}, is raised as {@link OrderDeclinedException} like the other two endpoints.
   */
  public QrSession createQrSession(CreateQrSessionRequest request, RequestOptions options) {
    ResourceSupport.requireNonNull(request, "request");
    BasketValidator.validate(request);
    HttpResult result = transport.post("/pay/qr/create", request, options);
    QrSession session;
    if (isDuplicateOnRetry(result)) {
      logRecoveredDuplicate(result);
      session = ResourceSupport.readData(result, QrSession.class);
    } else {
      ResourceSupport.ensureSuccess(transport, result);
      session = ResourceSupport.readData(result, QrSession.class);
    }
    if (session != null
        && (session.getQrCodeUrl() == null || session.getQrCodeUrl().isEmpty())
        && OrderStatus.DECLINED.equals(session.getStatus())) {
      throw new OrderDeclinedException(
          "The order was created but not approved, so there is no QR session to pay",
          ErrorMapper.ORDER_DECLINED,
          result.getStatus(),
          result.getRequestId(),
          null,
          result.getEnvelope() == null ? null : result.getEnvelope().getRaw(),
          session.getOrderId());
    }
    return session;
  }

  /**
   * The instalment plans the merchant's policy offers for {@code amount}, quoted with the client's
   * publishable key and language.
   *
   * @see #getPlans(BigDecimal, GetPlansOptions)
   */
  public Plans getPlans(BigDecimal amount) {
    return getPlans(amount, GetPlansOptions.defaults());
  }

  /**
   * The instalment plans the merchant's policy offers for {@code amount}: what the on-site widget
   * shows under a price, for a server that renders its own pages or decides whether to offer
   * Qasimha at all. The call authenticates with the publishable key only ({@code X-Public-Key});
   * the API key is not sent. {@link Plans#getPlans()} is empty, not an error, when the amount is
   * outside the policy's range or the account has no active policy.
   *
   * @throws ConfigurationException when neither the client nor {@code options} carries a
   *     publishable key, or the per-call key belongs to the other environment; no request is sent
   * @throws ValidationException when {@code amount} is not positive or has more than two decimals
   *     (client-side, no request is sent), or the API answers 400
   * @throws com.qasimha.sdk.exception.AuthenticationException when the API answers 401
   */
  public Plans getPlans(BigDecimal amount, GetPlansOptions options) {
    ResourceSupport.requireNonNull(amount, "amount");
    GetPlansOptions opts = options == null ? GetPlansOptions.defaults() : options;
    String key = opts.getPublishableKey();
    if (key != null && Environment.forPublishableKey(key) != environment) {
      throw new ConfigurationException(
          "publishableKey prefix "
              + Environment.forPublishableKey(key).getPublishableKeyPrefix()
              + " does not match the apiKey prefix "
              + environment.getKeyPrefix());
    }
    if (key == null) {
      key = transport.publishableKey();
    }
    if (key == null) {
      throw new ConfigurationException(
          "publishableKey is required for getPlans: set it on the builder or in GetPlansOptions");
    }
    if (amount.signum() <= 0) {
      throw ValidationException.clientSide(
          Collections.singletonList(new FieldError("Amount", "Must be greater than zero")));
    }
    if (!Money.hasAtMostTwoDecimals(amount)) {
      throw ValidationException.clientSide(
          Collections.singletonList(new FieldError("Amount", "Must have at most two decimals")));
    }
    String language = opts.getLanguage() != null ? opts.getLanguage() : transport.language();
    Map<String, String> query = new LinkedHashMap<>();
    query.put("amount", Money.plain(amount));
    query.put("currency", "SAR");
    query.put("lang", language);
    HttpResult result = transport.getPublic("/widget/plans", query, key, language);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, Plans.class);
  }

  private static boolean isDuplicateOnRetry(HttpResult result) {
    return result.getStatus() == 409
        && result.getAttempt() > 1
        && result.getEnvelope() != null
        && ErrorMapper.DUPLICATE_ORDER_REFERENCE.equals(result.getCode())
        && result.getEnvelope().dataText("installmentId") != null;
  }

  private void logRecoveredDuplicate(HttpResult result) {
    if (transport.logger() == null) {
      return;
    }
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("status", result.getStatus());
    fields.put("code", result.getCode());
    fields.put("attempt", result.getAttempt());
    if (result.getRequestId() != null) {
      fields.put("requestId", result.getRequestId());
    }
    transport
        .logger()
        .log(
            LogLevel.INFO,
            "duplicate order reference on a retry: the earlier attempt had succeeded",
            fields);
  }
}
