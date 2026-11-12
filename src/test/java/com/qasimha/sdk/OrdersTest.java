package com.qasimha.sdk;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qasimha.sdk.exception.ConfigurationException;
import com.qasimha.sdk.exception.NotFoundException;
import com.qasimha.sdk.model.CancelResult;
import com.qasimha.sdk.model.CaptureResult;
import com.qasimha.sdk.model.ContractDocument;
import com.qasimha.sdk.model.FulfillRequest;
import com.qasimha.sdk.model.FulfillResult;
import com.qasimha.sdk.model.ListOrdersParams;
import com.qasimha.sdk.model.Order;
import com.qasimha.sdk.model.OrderChannel;
import com.qasimha.sdk.model.OrderCounts;
import com.qasimha.sdk.model.OrderStatus;
import com.qasimha.sdk.model.OrderSummary;
import com.qasimha.sdk.model.Page;
import com.qasimha.sdk.model.PaymentType;
import com.qasimha.sdk.model.RefundRequest;
import com.qasimha.sdk.model.RefundResult;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrdersTest {

  private static final String ORDER_ID = "5c4a3eb5-c2b1-4d0f-b32a-062c379cfeac";
  private static final String REFERENCE = "CART-101412-9299-G";

  private static QasimhaClient client(MockApi api) {
    return QasimhaClient.builder().apiKey(Fixtures.TEST_KEY).baseUrl(api.baseUrl()).build();
  }

  @Test
  void listReadsTheRecordedFixture() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(200, Fixtures.read("orders-list.json"));
      Page<OrderSummary> page =
          client(api)
              .orders()
              .list(
                  ListOrdersParams.builder()
                      .status("captured")
                      .search("CART 1")
                      .page(2)
                      .pageSize(5)
                      .build());

      assertEquals("status=captured&search=CART+1&page=2&pageSize=5", api.lastRequest().query);
      assertEquals("/api/v1/merchant/orders", api.lastRequest().path);
      assertEquals(2, page.getItems().size());
      assertEquals(19, page.getPagination().getTotalItems());
      assertEquals(1, page.getPagination().getPage());
      assertEquals(2, page.getPagination().getPageSize());
      assertEquals(10, page.getPagination().getTotalPages());
      assertTrue(page.getPagination().hasNext());
      assertEquals("08bd4a65-79dd-46ee-8ee9-e6806313b740", page.getRequestId());
      assertTrue(page.getRaw().containsKey("pagination"));

      OrderSummary first = page.getItems().get(0);
      assertEquals(ORDER_ID, first.getId());
      assertEquals(REFERENCE, first.getOrderReferenceId());
      assertEquals(OrderStatus.DECLINED, first.getStatus());
      assertEquals("Declined", first.getStatus().getName());
      assertEquals(OrderChannel.ONLINE, first.getChannel());
      assertEquals("BNPL_POLICY_V41", first.getPolicyCode());
      assertEquals("BNPL", first.getPolicyType());
      assertEquals(0, new BigDecimal("250.00").compareTo(first.getTotalAmount()));
      assertEquals("SAR", first.getCurrencyCode());
      assertEquals(Instant.parse("2026-09-25T10:14:13.592218Z"), first.getCreatedAt());
      assertEquals("Noura Al-Shammari", first.getCustomerName());
      assertEquals("cart-shopper@qasimha.test", first.getCustomerEmail());
      assertEquals("+966512349999", first.getCustomerPhone());
      assertEquals(4, first.getInstalments());
      assertEquals(0, BigDecimal.ZERO.compareTo(first.getCapturedAmount()));
      assertNull(first.getEjarContractNumber());
      assertEquals("08bd4a65-79dd-46ee-8ee9-e6806313b740", first.getRequestId());
      assertEquals("Declined", first.getRaw().get("status"));
      assertEquals(0, new BigDecimal("1302.10").compareTo(page.getItems().get(1).getTotalAmount()));
    }
  }

  @Test
  void listAcceptsAnEmptyPageAndDefaults() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
          200,
          Fixtures.envelope(
              "SUCCESS",
              "{\"data\":[],\"pagination\":{\"totalItems\":0,\"page\":1,\"pageSize\":20,\"totalPages\":0}}"));
      Page<OrderSummary> page = client(api).orders().list();
      assertNull(api.lastRequest().query);
      assertTrue(page.getItems().isEmpty());
      assertFalse(page.getPagination().hasNext());
    }
  }

  @Test
  void getReadsTheTypedOrderAndRawCarriesNoDecisionInternals() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(200, Fixtures.read("order-detail.declined.json"));
      Order order = client(api).orders().get(ORDER_ID);

      assertEquals("/api/v1/merchant/orders/" + ORDER_ID, api.lastRequest().path);
      assertTypedDeclinedOrder(order);
      assertEquals("44d0220d-09a5-4c0e-9e08-288ca3f71f3c", order.getRequestId());
    }
  }

  private static void assertTypedDeclinedOrder(Order order) {
    assertEquals(ORDER_ID, order.getId());
    assertEquals(REFERENCE, order.getOrderReferenceId());
    assertEquals(OrderStatus.DECLINED, order.getStatus());
    assertEquals(PaymentType.PAY_BY_INSTALMENTS, order.getPaymentType());
    assertEquals(OrderChannel.ONLINE, order.getChannel());
    assertNull(order.getCheckoutChannel());
    assertEquals(0, new BigDecimal("250.00").compareTo(order.getTotalAmount()));
    assertEquals(0, BigDecimal.ZERO.compareTo(order.getTaxAmount()));
    assertEquals(0, BigDecimal.ZERO.compareTo(order.getShippingAmount()));
    assertEquals(0, BigDecimal.ZERO.compareTo(order.getDiscountAmount()));
    assertEquals("SAR", order.getCurrencyCode());
    assertEquals("SA", order.getCountryCode());
    assertEquals("Cart contract test", order.getDescription());
    assertEquals(4, order.getInstalments());
    assertEquals(0, BigDecimal.ZERO.compareTo(order.getDownPaymentAmount()));
    assertNull(order.getApprovedAmount());
    assertEquals(0, BigDecimal.ZERO.compareTo(order.getRefundableAmount()));
    assertEquals(0, BigDecimal.ZERO.compareTo(order.getCollectedAmount()));
    assertNull(order.getPaymentUrl());
    assertEquals(Instant.parse("2026-09-25T10:14:13.592218Z"), order.getCreatedAt());
    assertNull(order.getConfirmedAt());
    assertNull(order.getNotificationLanguage());
    assertNull(order.getContractNumber());

    assertEquals("Noura Al-Shammari", order.getCustomer().getName());
    assertEquals("cart-shopper@qasimha.test", order.getCustomer().getEmail());
    assertEquals("+966512349999", order.getCustomer().getPhone());
    assertEquals("d28f47d0-4435-4604-9b76-70d129c20372", order.getCustomer().getId());
    assertNull(order.getCustomer().getNationalId());

    assertEquals(1, order.getItems().size());
    assertEquals("SKU-AR", order.getItems().get(0).getSku());
    assertEquals("سماعات لاسلكية", order.getItems().get(0).getName());
    assertEquals(1, order.getItems().get(0).getQuantity());
    assertEquals(0, new BigDecimal("250.00").compareTo(order.getItems().get(0).getUnitPrice()));
    assertEquals(0, new BigDecimal("250.00").compareTo(order.getItems().get(0).getTotalAmount()));
    assertEquals("physical", order.getItems().get(0).getType());

    assertTrue(order.getInstallments().isEmpty());
    assertTrue(order.getFulfillments().isEmpty());
    assertTrue(order.getReversals().isEmpty());
    assertTrue(order.getVoids().isEmpty());
    assertEquals(3, order.getStatusHistory().size());
    assertEquals(OrderStatus.DRAFT, order.getStatusHistory().get(2).getFromStatus());
    assertEquals(OrderStatus.DECLINED, order.getStatusHistory().get(2).getToStatus());
    assertEquals("DecisionDeclined", order.getStatusHistory().get(2).getTrigger());
    assertEquals("DecisionEvaluated", order.getStatusHistory().get(0).getTrigger());
    assertNull(order.getStatusHistory().get(0).getMetadata());

    Map<String, Object> raw = order.getRaw();
    assertEquals("Declined", raw.get("status"));
    assertEquals("PayByInstalments", raw.get("paymentType"));
    assertTrue(raw.containsKey("statusHistory"));
    assertFalse(raw.containsKey("declinedCode"));
    assertFalse(raw.containsKey("decisionGateTrace"));
    assertFalse(raw.containsKey("decisionStatus"));
    assertFalse(raw.containsKey("lynkRequired"));
    assertFalse(raw.containsKey("domainEvents"));
  }

  @Test
  void getByReferenceReadsTheSameOrder() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(200, Fixtures.read("order-by-reference.json"));
      Order order = client(api).orders().getByReference(REFERENCE);

      assertEquals("GET", api.lastRequest().method);
      assertEquals("/api/v1/merchant/orders/by-reference/" + REFERENCE, api.lastRequest().path);
      assertNull(api.lastRequest().query);
      assertNull(api.lastRequest().header("Idempotency-Key"));
      assertTypedDeclinedOrder(order);
      assertEquals("22e61620-9158-4afd-ac79-a362a10ac7d5", order.getRequestId());
    }
  }

  @Test
  void getByReferenceEncodesEachSegmentAndKeepsSlashes() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(200, Fixtures.read("order-by-reference.json"))
          .enqueue(200, Fixtures.read("order-by-reference.json"))
          .enqueue(200, Fixtures.read("order-by-reference.json"));
      Orders orders = client(api).orders();

      orders.getByReference("ORDER/2026/1");
      assertEquals("/api/v1/merchant/orders/by-reference/ORDER/2026/1", api.lastRequest().rawPath);

      orders.getByReference(" a b#c?d/ü/x%y ");
      assertEquals(
          "/api/v1/merchant/orders/by-reference/a%20b%23c%3Fd/%C3%BC/x%25y",
          api.lastRequest().rawPath);
      assertNull(api.lastRequest().query);

      orders.getByReference("ORDER//1/");
      assertEquals("/api/v1/merchant/orders/by-reference/ORDER//1/", api.lastRequest().rawPath);
    }
  }

  @Test
  void getByReferenceRefusesABlankReferenceAndMapsNotFound() throws Exception {
    try (MockApi api = new MockApi()) {
      Orders orders = client(api).orders();
      assertThrows(ConfigurationException.class, () -> orders.getByReference(null));
      assertThrows(ConfigurationException.class, () -> orders.getByReference("  "));
      assertEquals(0, api.requestCount());

      api.enqueue(404, Fixtures.failure("NOT_FOUND", "Order not found", null, null));
      NotFoundException e =
          assertThrows(NotFoundException.class, () -> orders.getByReference("NOPE-1"));
      assertEquals("NOT_FOUND", e.getCode());
      assertEquals(404, e.getHttpStatus());
      assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", e.getRequestId());
    }
  }

  @Test
  void getByReferenceLogsThePathTemplateNotTheReference() throws Exception {
    try (MockApi api = new MockApi()) {
      RecordingLogger logger = new RecordingLogger();
      api.enqueue(200, Fixtures.read("order-by-reference.json"));
      QasimhaClient.builder()
          .apiKey(Fixtures.TEST_KEY)
          .baseUrl(api.baseUrl())
          .logger(logger)
          .build()
          .orders()
          .getByReference("ORDER/NOURA/0551234567");
      assertFalse(logger.dump().contains("NOURA"), logger.dump());
      assertEquals(
          "/api/v1/merchant/orders/by-reference/{orderReferenceId}",
          logger.entries().get(0).fields.get("path"));
      assertEquals(200, logger.entries().get(0).fields.get("status"));
    }
  }

  @Test
  void enumNamesAreAcceptedAndUnknownValuesPassThrough() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
          200,
          Fixtures.envelope(
              "SUCCESS",
              "{\"id\":\"o1\",\"status\":\"Confirmed\",\"paymentType\":\"PayNow\",\"channel\":\"QrCode\",\"items\":[],"
                  + "\"installments\":[{\"sequence\":1,\"dueDate\":\"2026-10-25T00:00:00\",\"amount\":300.00,\"status\":1},"
                  + "{\"sequence\":2,\"dueDate\":\"2026-11-25\",\"amount\":300.00,\"status\":\"Late\"},"
                  + "{\"sequence\":3,\"dueDate\":\"2026-12-25T00:00:00Z\",\"amount\":300.00,\"status\":42}],"
                  + "\"statusHistory\":[{\"fromStatus\":null,\"toStatus\":\"SomethingNew\",\"trigger\":\"x\"}]}"));
      api.enqueue(
          200,
          Fixtures.envelope(
              "SUCCESS", "{\"id\":\"o2\",\"status\":99,\"channel\":7,\"paymentType\":\"3\"}"));

      Order order = client(api).orders().get("o1");
      assertEquals(OrderStatus.CONFIRMED, order.getStatus());
      assertEquals(PaymentType.PAY_NOW, order.getPaymentType());
      assertEquals(OrderChannel.QR_CODE, order.getChannel());
      assertEquals("Paid", order.getInstallments().get(0).getStatus().getName());
      assertEquals("Late", order.getInstallments().get(1).getStatus().getName());
      assertEquals("42", order.getInstallments().get(2).getStatus().getName());
      assertFalse(order.getInstallments().get(2).getStatus().isKnown());
      assertEquals(
          Instant.parse("2026-10-25T00:00:00Z"), order.getInstallments().get(0).getDueDate());
      assertEquals(
          Instant.parse("2026-11-25T00:00:00Z"), order.getInstallments().get(1).getDueDate());
      assertNull(order.getStatusHistory().get(0).getFromStatus());
      assertEquals("SomethingNew", order.getStatusHistory().get(0).getToStatus().getName());
      assertFalse(order.getStatusHistory().get(0).getToStatus().isKnown());

      Order unknown = client(api).orders().get("o2");
      assertEquals("99", unknown.getStatus().getName());
      assertFalse(unknown.getStatus().isKnown());
      assertNull(unknown.getStatus().getCode());
      assertEquals("7", unknown.getChannel().getName());
      assertEquals(PaymentType.PAY_IN_FULL, unknown.getPaymentType());
    }
  }

  @Test
  void captureCancelRefundFulfilAndCounts() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
              200,
              Fixtures.envelope(
                  "SUCCESS",
                  "{\"id\":\""
                      + ORDER_ID
                      + "\",\"status\":\"Confirmed\",\"confirmedAt\":\"2026-06-11T09:30:00Z\"}"))
          .enqueue(200, Fixtures.envelope("SUCCESS", "{\"id\":\"" + ORDER_ID + "\",\"status\":10}"))
          .enqueue(
              200,
              Fixtures.envelope(
                  "SUCCESS",
                  "{\"id\":\"" + ORDER_ID + "\",\"status\":9,\"refundedAmount\":300.00}"))
          .enqueue(
              200,
              Fixtures.envelope(
                  "SUCCESS",
                  "{\"id\":\""
                      + ORDER_ID
                      + "\",\"status\":\"PartiallyFulfilled\",\"fulfilledAmount\":1200.00}"))
          .enqueue(
              200,
              Fixtures.envelope(
                  "SUCCESS",
                  "{\"id\":\""
                      + ORDER_ID
                      + "\",\"status\":\"FullyFulfilled\",\"fulfilledAmount\":1302.10}"))
          .enqueue(
              200,
              Fixtures.envelope(
                  "SUCCESS",
                  "{\"all\":7,\"approved\":1,\"authorised\":2,\"captured\":3,\"refunded\":0,\"cancelled\":1}"));
      Orders orders = client(api).orders();

      CaptureResult capture = orders.capture(ORDER_ID);
      assertEquals(OrderStatus.CONFIRMED, capture.getStatus());
      assertEquals(Instant.parse("2026-06-11T09:30:00Z"), capture.getConfirmedAt());
      assertEquals("/api/v1/merchant/orders/" + ORDER_ID + "/capture", api.lastRequest().path);
      assertEquals("POST", api.lastRequest().method);
      assertNotNull(api.lastRequest().header("Idempotency-Key"));
      assertEquals("", api.lastRequest().bodyText());

      CancelResult cancel = orders.cancel(ORDER_ID);
      assertEquals(OrderStatus.VOIDED, cancel.getStatus());
      assertEquals("/api/v1/merchant/orders/" + ORDER_ID + "/cancel", api.lastRequest().path);

      RefundResult refund =
          orders.refund(ORDER_ID, RefundRequest.of(new BigDecimal("300.00"), "Customer return"));
      assertEquals(OrderStatus.PARTIALLY_REVERSED, refund.getStatus());
      assertEquals(0, new BigDecimal("300.00").compareTo(refund.getRefundedAmount()));
      assertEquals(
          "{\"amount\":300.00,\"reason\":\"Customer return\"}", api.lastRequest().bodyText());

      FulfillResult partial =
          orders.fulfill(
              ORDER_ID,
              FulfillRequest.builder()
                  .shippedAmount(new BigDecimal("1200.00"))
                  .shippingCompany("Aramex")
                  .trackingNumber("ARX123")
                  .build());
      assertEquals(OrderStatus.PARTIALLY_FULFILLED, partial.getStatus());
      assertEquals(
          "{\"shippedAmount\":1200.00,\"shippingCompany\":\"Aramex\",\"trackingNumber\":\"ARX123\"}",
          api.lastRequest().bodyText());

      FulfillResult all = orders.fulfill(ORDER_ID, FulfillRequest.all());
      assertEquals(OrderStatus.FULLY_FULFILLED, all.getStatus());
      assertEquals("{}", api.lastRequest().bodyText());

      OrderCounts counts = orders.counts();
      assertEquals("/api/v1/merchant/orders/counts", api.lastRequest().path);
      assertEquals(7, counts.getAll());
      assertEquals(1, counts.getApproved());
      assertEquals(2, counts.getAuthorised());
      assertEquals(3, counts.getCaptured());
      assertEquals(0, counts.getRefunded());
      assertEquals(1, counts.getCancelled());
    }
  }

  @Test
  void contractDownloadsBytes() throws Exception {
    try (MockApi api = new MockApi()) {
      byte[] pdf = "%PDF-1.7 fake".getBytes(StandardCharsets.US_ASCII);
      api.enqueueBytes(
          200,
          Map.of(
              "Content-Type",
              "application/pdf",
              "Content-Disposition",
              "attachment; filename=\"contract-1.pdf\"",
              "X-Request-Id",
              "req-77"),
          pdf);
      ContractDocument document = client(api).orders().contract(ORDER_ID);
      assertArrayEquals(pdf, document.getContent());
      assertEquals("application/pdf", document.getContentType());
      assertEquals("contract-1.pdf", document.getFileName());
      assertEquals("req-77", document.getRequestId());
      assertEquals("/api/v1/merchant/orders/" + ORDER_ID + "/contract", api.lastRequest().path);
      assertTrue(api.lastRequest().header("Accept").startsWith("application/pdf"));
    }
  }

  @Test
  void blankIdsAreRefusedBeforeSending() throws Exception {
    try (MockApi api = new MockApi()) {
      Orders orders = client(api).orders();
      assertThrows(ConfigurationException.class, () -> orders.get(" "));
      assertThrows(ConfigurationException.class, () -> orders.capture(null));
      assertThrows(ConfigurationException.class, () -> orders.contract(""));
      assertEquals(0, api.requestCount());
    }
  }

  @Test
  void idsAreUrlEncoded() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(200, Fixtures.envelope("SUCCESS", "{\"id\":\"a b\"}"));
      client(api).orders().get("a b/../c");
      assertEquals("/api/v1/merchant/orders/a%20b%2F..%2Fc", api.lastRequest().rawPath);
    }
  }
}
