package com.qasimha.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qasimha.sdk.exception.ConflictException;
import com.qasimha.sdk.exception.DuplicateOrderReferenceException;
import com.qasimha.sdk.model.CheckoutLink;
import com.qasimha.sdk.model.CheckoutLinkChannel;
import com.qasimha.sdk.model.CheckoutLinkItem;
import com.qasimha.sdk.model.CreateCheckoutLinkRequest;
import com.qasimha.sdk.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckoutLinksTest {

  private static final String LINK_ID = "3f0b9c2e-5555-4a6b-9c1d-000000000009";

  private static QasimhaClient client(MockApi api) {
    return QasimhaClient.builder().apiKey(Fixtures.TEST_KEY).baseUrl(api.baseUrl()).build();
  }

  @Test
  void createReturnsTheHostedPage() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
          201,
          Fixtures.envelope(
              "LINK_CREATED",
              "{\"id\":\""
                  + LINK_ID
                  + "\",\"url\":\"https://checkout.example.test/pay/"
                  + LINK_ID
                  + "\",\"channel\":\"Link\",\"amount\":250.00,\"currency\":\"SAR\",\"phone\":\"+966512349999\",\"language\":\"ar\","
                  + "\"orderReferenceId\":\"MERCHANT-ORDER-12345\",\"ejarContractNumber\":null,"
                  + "\"items\":[{\"name\":\"Kettle\",\"sku\":\"K1\",\"quantity\":2,\"unitPrice\":125.00,\"total\":250.00,\"imageUrl\":null}],"
                  + "\"status\":\"AwaitingCheckout\",\"expiresAt\":\"2026-09-25T11:00:00Z\"}"));
      CheckoutLink link =
          client(api)
              .checkoutLinks()
              .create(
                  CreateCheckoutLinkRequest.builder()
                      .amount(new BigDecimal("250.00"))
                      .channel(CheckoutLinkChannel.LINK)
                      .phone("+966512349999")
                      .language("ar")
                      .orderReferenceId("MERCHANT-ORDER-12345")
                      .addItem(
                          CheckoutLinkItem.builder()
                              .name("Kettle")
                              .sku("K1")
                              .quantity(2)
                              .unitPrice(new BigDecimal("125.00"))
                              .build())
                      .build());

      assertEquals("/api/v1/merchant/checkout-links", api.lastRequest().path);
      String body = api.lastRequest().bodyText();
      assertTrue(body.contains("\"amount\":250.00"), body);
      assertTrue(body.contains("\"channel\":\"Link\""), body);
      assertTrue(body.contains("\"phone\":\"+966512349999\""), body);
      assertTrue(body.contains("\"language\":\"ar\""), body);
      assertFalse(body.contains("\"total\""), body);
      assertFalse(body.contains("ejarContractNumber"), body);

      assertEquals(LINK_ID, link.getId());
      assertEquals("https://checkout.example.test/pay/" + LINK_ID, link.getUrl());
      assertEquals("Link", link.getChannel());
      assertEquals(0, new BigDecimal("250.00").compareTo(link.getAmount()));
      assertEquals("SAR", link.getCurrency());
      assertEquals("ar", link.getLanguage());
      assertEquals(OrderStatus.AWAITING_CHECKOUT, link.getStatus());
      assertEquals(Instant.parse("2026-09-25T11:00:00Z"), link.getExpiresAt());
      assertEquals(1, link.getItems().size());
      assertEquals(0, new BigDecimal("250.00").compareTo(link.getItems().get(0).getTotal()));
      assertNull(link.getCompleted());
      assertNull(link.getWaiting());
    }
  }

  @Test
  void qrChannelNeedsNoPhone() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
          201,
          Fixtures.envelope(
              "LINK_CREATED",
              "{\"id\":\""
                  + LINK_ID
                  + "\",\"url\":\"https://c/pay/"
                  + LINK_ID
                  + "\",\"channel\":\"Qr\",\"status\":\"AwaitingCheckout\"}"));
      CheckoutLink link =
          client(api)
              .checkoutLinks()
              .create(
                  CreateCheckoutLinkRequest.builder()
                      .amount(new BigDecimal("18000.00"))
                      .channel(CheckoutLinkChannel.QR)
                      .ejarContractNumber("EJ-2026-000123")
                      .build());
      assertEquals("Qr", link.getChannel());
      assertTrue(
          api.lastRequest().bodyText().contains("\"ejarContractNumber\":\"EJ-2026-000123\""));
    }
  }

  @Test
  void listGetAndCancel() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
              200,
              Fixtures.envelope(
                  "SUCCESS",
                  "{\"items\":[{\"id\":\""
                      + LINK_ID
                      + "\",\"orderReferenceId\":\"LINK-20260925-ABCDEF\",\"amount\":250.00,\"currency\":\"SAR\",\"phone\":\"*****999\","
                      + "\"channel\":\"Link\",\"status\":\"Expired\",\"decisionStatus\":null,\"expiresAt\":\"2026-09-25T11:00:00Z\","
                      + "\"createdAt\":\"2026-09-25T09:00:00Z\",\"waiting\":false}]}"))
          .enqueue(
              200,
              Fixtures.envelope(
                  "SUCCESS",
                  "{\"id\":\""
                      + LINK_ID
                      + "\",\"orderReferenceId\":\"LINK-20260925-ABCDEF\",\"amount\":250.00,\"currency\":\"SAR\",\"phone\":\"*****999\","
                      + "\"channel\":\"Link\",\"status\":\"Confirmed\",\"decisionStatus\":\"APPROVED\",\"expiresAt\":null,"
                      + "\"createdAt\":\"2026-09-25T09:00:00Z\",\"completed\":true,\"declined\":false}"))
          .enqueue(
              409,
              Fixtures.failure("NOT_OPEN", "Cannot cancel \\u2014 status Confirmed.", null, null));
      CheckoutLinks links = client(api).checkoutLinks();

      List<CheckoutLink> list = links.list();
      assertEquals("/api/v1/merchant/checkout-links", api.lastRequest().path);
      assertEquals(1, list.size());
      assertEquals(OrderStatus.EXPIRED, list.get(0).getStatus());
      assertEquals(Boolean.FALSE, list.get(0).getWaiting());
      assertEquals("*****999", list.get(0).getPhone());
      assertEquals("11111111-2222-3333-4444-555555555555", list.get(0).getRequestId());

      CheckoutLink link = links.get(LINK_ID);
      assertEquals("/api/v1/merchant/checkout-links/" + LINK_ID, api.lastRequest().path);
      assertEquals(Boolean.TRUE, link.getCompleted());
      assertEquals(Boolean.FALSE, link.getDeclined());
      assertEquals("APPROVED", link.getDecisionStatus());
      assertNull(link.getUrl());

      ConflictException e = assertThrows(ConflictException.class, () -> links.cancel(LINK_ID));
      assertEquals("NOT_OPEN", e.getCode());
      assertEquals(409, e.getHttpStatus());
      assertFalse(e instanceof DuplicateOrderReferenceException);
      assertEquals(
          "/api/v1/merchant/checkout-links/" + LINK_ID + "/cancel", api.lastRequest().path);
    }
  }
}
