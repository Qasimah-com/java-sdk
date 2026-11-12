package com.qasimha.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qasimha.sdk.exception.ApiException;
import com.qasimha.sdk.exception.DuplicateOrderReferenceException;
import com.qasimha.sdk.exception.OrderDeclinedException;
import com.qasimha.sdk.model.CreatePosSessionRequest;
import com.qasimha.sdk.model.CreateQrSessionRequest;
import com.qasimha.sdk.model.CreateSessionRequest;
import com.qasimha.sdk.model.Item;
import com.qasimha.sdk.model.MerchantUrl;
import com.qasimha.sdk.model.OrderStatus;
import com.qasimha.sdk.model.PosSession;
import com.qasimha.sdk.model.QrSession;
import com.qasimha.sdk.model.RiskAssessment;
import com.qasimha.sdk.model.Session;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CheckoutTest {

  private static final String ORDER_ID = "9a2e6c4b-1111-4c2d-8a1f-000000000001";

  private static final String SESSION =
      Fixtures.envelope(
          "SESSION_CREATED",
          "{\"installmentId\":\""
              + ORDER_ID
              + "\",\"providerSessionId\":\"sess_1\",\"paymentUrl\":\"https://pay.example.test/x\",\"status\":\"New\"}");

  private static QasimhaClient client(MockApi api, RecordingLogger logger) {
    return QasimhaClient.builder()
        .apiKey(Fixtures.TEST_KEY)
        .baseUrl(api.baseUrl())
        .sleeper(new RecordingSleeper())
        .logger(logger)
        .build();
  }

  @Test
  void createdSessionCarriesUrlIdStatusAndRequestId() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(201, SESSION);
      CreateSessionRequest request =
          Fixtures.threeLineBasket("CART-1")
              .merchantUrl(
                  MerchantUrl.builder()
                      .success("https://store.example.test/ok")
                      .failure("https://store.example.test/fail")
                      .cancel("https://store.example.test/cancel")
                      .build())
              .riskAssessment(RiskAssessment.builder().isExistingCustomer(true).build())
              .build();
      Session session = client(api, null).checkout().createSession(request);

      assertEquals(ORDER_ID, session.getOrderId());
      assertEquals("https://pay.example.test/x", session.getPaymentUrl());
      assertEquals("sess_1", session.getProviderSessionId());
      assertEquals(OrderStatus.NEW, session.getStatus());
      assertEquals("11111111-2222-3333-4444-555555555555", session.getRequestId());
      assertFalse(session.isReplayed());
      assertEquals(ORDER_ID, session.getRaw().get("installmentId"));

      String body = api.lastRequest().bodyText();
      assertTrue(body.contains("\"totalAmount\":1302.10"), body);
      assertTrue(body.contains("\"taxAmount\":173.10"), body);
      assertTrue(body.contains("\"paymentType\":\"pay_by_instalments\""), body);
      assertTrue(body.contains("\"instalments\":4"), body);
      assertTrue(body.contains("\"currencyCode\":\"SAR\""), body);
      assertTrue(body.contains("\"countryCode\":\"SA\""), body);
      assertTrue(body.contains("\"unitPrice\":45.00"), body);
      assertTrue(body.contains("\"totalAmount\":135.00"), body);
      assertTrue(body.contains("\"type\":\"digital\""), body);
      assertTrue(
          body.contains("\"merchantUrl\":{\"success\":\"https://store.example.test/ok\""), body);
      assertTrue(body.contains("\"isExistingCustomer\":true"), body);
      assertTrue(body.contains("\"shippingAddress\":{"), body);
      assertFalse(body.contains("merchantId"), body);
      assertFalse(body.contains("decisionFeatures"), body);
      assertFalse(body.contains("billingAddress"), body);
    }
  }

  @Test
  void duplicateReferenceOnFirstAttemptIsAnError() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
          409,
          Fixtures.failure(
              "DUPLICATE_ORDER_REFERENCE",
              "Order reference already in use",
              "{\"installmentId\":\""
                  + ORDER_ID
                  + "\",\"paymentUrl\":\"https://pay.example.test/x\"}",
              null));
      DuplicateOrderReferenceException e =
          assertThrows(
              DuplicateOrderReferenceException.class,
              () ->
                  client(api, null)
                      .checkout()
                      .createSession(Fixtures.threeLineBasket("CART-2").build()));
      assertEquals(ORDER_ID, e.getExistingOrderId());
      assertEquals("https://pay.example.test/x", e.getExistingPaymentUrl());
      assertEquals("DUPLICATE_ORDER_REFERENCE", e.getCode());
      assertEquals(409, e.getHttpStatus());
      assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", e.getRequestId());
      assertEquals("Order reference already in use", e.getMessage());
      assertEquals("DUPLICATE_ORDER_REFERENCE", e.getRawEnvelope().get("code"));
      assertEquals(1, api.requestCount());
    }
  }

  @Test
  void duplicateReferenceOnRetryIsTheEarlierSuccess() throws Exception {
    try (MockApi api = new MockApi()) {
      RecordingLogger logger = new RecordingLogger();
      api.enqueue(503, "")
          .enqueue(
              409,
              Fixtures.failure(
                  "DUPLICATE_ORDER_REFERENCE",
                  "Order reference already in use",
                  "{\"installmentId\":\""
                      + ORDER_ID
                      + "\",\"paymentUrl\":\"https://pay.example.test/x\",\"status\":\"New\"}",
                  null));
      Session session =
          client(api, logger).checkout().createSession(Fixtures.threeLineBasket("CART-3").build());
      assertEquals(ORDER_ID, session.getOrderId());
      assertEquals("https://pay.example.test/x", session.getPaymentUrl());
      assertEquals(OrderStatus.NEW, session.getStatus());
      assertEquals(2, api.requestCount());
      assertEquals(
          api.requests().get(0).header("Idempotency-Key"),
          api.requests().get(1).header("Idempotency-Key"));
      assertTrue(
          logger.entries().stream()
              .anyMatch(
                  e -> e.level == LogLevel.INFO && e.message.contains("duplicate order reference")),
          logger.dump());
    }
  }

  @Test
  void duplicateReferenceOnRetryWithoutIdIsStillAnError() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(503, "")
          .enqueue(
              409,
              Fixtures.failure(
                  "DUPLICATE_ORDER_REFERENCE", "Order reference already in use", null, null));
      assertThrows(
          DuplicateOrderReferenceException.class,
          () ->
              client(api, null)
                  .checkout()
                  .createSession(Fixtures.threeLineBasket("CART-4").build()));
    }
  }

  @Test
  void declinedOrderIsAnErrorCarryingTheOrderId() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
          422,
          Fixtures.failure(
              "ORDER_DECLINED",
              "The order was not approved",
              "{\"installmentId\":\"" + ORDER_ID + "\"}",
              null));
      OrderDeclinedException e =
          assertThrows(
              OrderDeclinedException.class,
              () ->
                  client(api, null)
                      .checkout()
                      .createSession(Fixtures.threeLineBasket("CART-5").build()));
      assertEquals(ORDER_ID, e.getOrderId());
      assertEquals(422, e.getHttpStatus());
      assertEquals("ORDER_DECLINED", e.getCode());
      assertEquals("The order was not approved", e.getMessage());
    }
  }

  @Test
  void twoHundredWithSuccessFalseIsAnError() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(200, Fixtures.failure("SOMETHING_ODD", "Not really ok", null, null));
      ApiException e =
          assertThrows(
              ApiException.class,
              () ->
                  client(api, null)
                      .checkout()
                      .createSession(Fixtures.threeLineBasket("CART-6").build()));
      assertEquals("SOMETHING_ODD", e.getCode());
      assertEquals(200, e.getHttpStatus());
    }
  }

  @Test
  void posSessionReturnsTheHostedPage() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
          201,
          Fixtures.envelope(
              "SESSION_CREATED",
              "{\"installmentId\":\""
                  + ORDER_ID
                  + "\",\"providerSessionId\":null,\"checkoutUrl\":\"https://checkout.example.test/pay/"
                  + ORDER_ID
                  + "\",\"status\":\"AwaitingCheckout\"}"));
      CreatePosSessionRequest request =
          CreatePosSessionRequest.builder()
              .orderReferenceId("POS-1")
              .totalAmount(new BigDecimal("250.00"))
              .addItem(Fixtures.item("SKU-P", "Kettle", Item.TYPE_PHYSICAL, 1, "250.00"))
              .consumer(Fixtures.consumer())
              .build();
      PosSession session = client(api, null).checkout().createPosSession(request);
      assertEquals(ORDER_ID, session.getOrderId());
      assertEquals("https://checkout.example.test/pay/" + ORDER_ID, session.getCheckoutUrl());
      assertEquals(OrderStatus.AWAITING_CHECKOUT, session.getStatus());
      assertNull(session.getProviderSessionId());
      assertEquals("/api/v1/pay/pos/create", api.lastRequest().path);
      assertTrue(api.lastRequest().bodyText().contains("\"email\":\"cart-shopper@qasimha.test\""));
    }
  }

  @Test
  void qrSessionReturnsTheQrUrl() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
          201,
          Fixtures.envelope(
              "SESSION_CREATED",
              "{\"installmentId\":\""
                  + ORDER_ID
                  + "\",\"providerSessionId\":\"qr_1\",\"qrCodeUrl\":\"https://checkout.example.test/qr/1\",\"status\":\"New\"}"));
      QrSession session = client(api, null).checkout().createQrSession(qrRequest("QR-1"));
      assertEquals("https://checkout.example.test/qr/1", session.getQrCodeUrl());
      assertEquals(OrderStatus.NEW, session.getStatus());
      assertEquals("/api/v1/pay/qr/create", api.lastRequest().path);
      assertFalse(api.lastRequest().bodyText().contains("consumer"));
    }
  }

  @Test
  void declinedQrSessionIsNormalisedToOrderDeclined() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
          201,
          Fixtures.envelope(
              "SESSION_CREATED",
              "{\"installmentId\":\""
                  + ORDER_ID
                  + "\",\"providerSessionId\":null,\"qrCodeUrl\":\"\",\"status\":\"Declined\"}"));
      OrderDeclinedException e =
          assertThrows(
              OrderDeclinedException.class,
              () -> client(api, null).checkout().createQrSession(qrRequest("QR-2")));
      assertEquals(ORDER_ID, e.getOrderId());
      assertEquals("ORDER_DECLINED", e.getCode());
      assertEquals(201, e.getHttpStatus());
      assertEquals("11111111-2222-3333-4444-555555555555", e.getRequestId());
    }
  }

  private static CreateQrSessionRequest qrRequest(String reference) {
    return CreateQrSessionRequest.builder()
        .orderReferenceId(reference)
        .totalAmount(new BigDecimal("250.00"))
        .addItem(Fixtures.item("SKU-Q", "Kettle", Item.TYPE_PHYSICAL, 1, "250.00"))
        .build();
  }
}
