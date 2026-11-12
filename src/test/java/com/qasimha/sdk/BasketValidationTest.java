package com.qasimha.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qasimha.sdk.exception.ValidationException;
import com.qasimha.sdk.model.CheckoutLinkChannel;
import com.qasimha.sdk.model.Consumer;
import com.qasimha.sdk.model.CreateCheckoutLinkRequest;
import com.qasimha.sdk.model.CreatePosSessionRequest;
import com.qasimha.sdk.model.CreateSessionRequest;
import com.qasimha.sdk.model.FieldError;
import com.qasimha.sdk.model.FulfillRequest;
import com.qasimha.sdk.model.Item;
import com.qasimha.sdk.model.RefundRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class BasketValidationTest {

  private static List<String> fields(ValidationException e) {
    return e.getErrors().stream().map(FieldError::getField).collect(Collectors.toList());
  }

  private static ValidationException reject(MockApi api, CreateSessionRequest request) {
    ValidationException e =
        assertThrows(
            ValidationException.class,
            () ->
                QasimhaClient.builder()
                    .apiKey(Fixtures.TEST_KEY)
                    .baseUrl(api.baseUrl())
                    .build()
                    .checkout()
                    .createSession(request));
    assertTrue(e.isClientSide());
    assertNull(e.getHttpStatus());
    assertEquals(0, api.requestCount(), "no request may be sent");
    return e;
  }

  @Test
  void validBasketIsSent() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
          201,
          Fixtures.envelope(
              "SESSION_CREATED",
              "{\"installmentId\":\"1\",\"paymentUrl\":\"https://p\",\"status\":\"New\"}"));
      QasimhaClient.builder()
          .apiKey(Fixtures.TEST_KEY)
          .baseUrl(api.baseUrl())
          .build()
          .checkout()
          .createSession(Fixtures.threeLineBasket("CART-V-1").build());
      assertEquals(1, api.requestCount());
    }
  }

  @Test
  void itemBuilderComputesTheLineTotal() {
    Item item =
        Item.builder()
            .referenceId("A")
            .name("A")
            .type(Item.TYPE_PHYSICAL)
            .quantity(3)
            .unitPrice(new BigDecimal("45.00"))
            .discountAmount(new BigDecimal("5.00"))
            .build();
    assertEquals(0, new BigDecimal("130.00").compareTo(item.getTotalAmount()));
  }

  @Test
  void lineThatDoesNotAddUpIsNamed() throws Exception {
    try (MockApi api = new MockApi()) {
      CreateSessionRequest request =
          Fixtures.threeLineBasket("CART-V-2")
              .items(
                  List.of(
                      Item.builder()
                          .referenceId("SKU-9")
                          .name("Laptop")
                          .type(Item.TYPE_PHYSICAL)
                          .quantity(1)
                          .unitPrice(new BigDecimal("5000.00"))
                          .totalAmount(new BigDecimal("10.00"))
                          .build()))
              .build();
      ValidationException e = reject(api, request);
      assertTrue(fields(e).contains("Items[0].TotalAmount"), e.toString());
      assertTrue(e.getMessage().contains("Items[0].TotalAmount"), e.getMessage());
    }
  }

  @Test
  void cartThatDoesNotAddUpIsNamed() throws Exception {
    try (MockApi api = new MockApi()) {
      CreateSessionRequest request =
          Fixtures.threeLineBasket("CART-V-3").totalAmount(new BigDecimal("10.00")).build();
      ValidationException e = reject(api, request);
      assertEquals(List.of("TotalAmount"), fields(e));
      assertTrue(e.getErrors().get(0).getMessage().contains("1302.10"), e.toString());
    }
  }

  @Test
  void identitiesHoldToTheHalala() throws Exception {
    try (MockApi api = new MockApi()) {
      CreateSessionRequest request =
          Fixtures.threeLineBasket("CART-V-4").totalAmount(new BigDecimal("1302.11")).build();
      assertEquals(List.of("TotalAmount"), fields(reject(api, request)));
    }
  }

  @Test
  void moreThanTwoDecimalsIsRefused() throws Exception {
    try (MockApi api = new MockApi()) {
      CreateSessionRequest request =
          Fixtures.threeLineBasket("CART-V-5").totalAmount(new BigDecimal("1302.105")).build();
      ValidationException e = reject(api, request);
      assertEquals(List.of("TotalAmount"), fields(e));
      assertTrue(e.getErrors().get(0).getMessage().contains("two decimals"));
    }
  }

  @Test
  void emptyCartZeroQuantityAndNegativePriceAreRefused() throws Exception {
    try (MockApi api = new MockApi()) {
      assertEquals(
          List.of("Items"),
          fields(reject(api, Fixtures.threeLineBasket("D").items(List.of()).build())));

      ValidationException zero =
          reject(
              api,
              Fixtures.threeLineBasket("E")
                  .totalAmount(new BigDecimal("100"))
                  .taxAmount(BigDecimal.ZERO)
                  .shippingAmount(BigDecimal.ZERO)
                  .discountAmount(BigDecimal.ZERO)
                  .items(List.of(Fixtures.item("SKU-0", "Ghost", Item.TYPE_PHYSICAL, 0, "100.00")))
                  .build());
      assertTrue(fields(zero).contains("Items[0].Quantity"), zero.toString());

      ValidationException negative =
          reject(
              api,
              Fixtures.threeLineBasket("E2")
                  .totalAmount(new BigDecimal("100"))
                  .taxAmount(BigDecimal.ZERO)
                  .shippingAmount(BigDecimal.ZERO)
                  .discountAmount(BigDecimal.ZERO)
                  .items(
                      List.of(Fixtures.item("SKU-N", "Negative", Item.TYPE_PHYSICAL, 1, "-100.00")))
                  .build());
      assertTrue(fields(negative).contains("Items[0].UnitPrice"), negative.toString());
    }
  }

  @Test
  void requiredFieldsAreNamed() throws Exception {
    try (MockApi api = new MockApi()) {
      ValidationException e =
          reject(
              api,
              Fixtures.threeLineBasket("F").shippingAddress(null).paymentType("later").build());
      assertTrue(fields(e).contains("ShippingAddress"), e.toString());
      assertTrue(fields(e).contains("PaymentType"), e.toString());

      ValidationException blank =
          reject(
              api,
              Fixtures.threeLineBasket("")
                  .consumer(Consumer.builder().firstName("Noura").build())
                  .build());
      assertTrue(fields(blank).contains("OrderReferenceId"), blank.toString());
      assertTrue(fields(blank).contains("Consumer.LastName"), blank.toString());
      assertTrue(fields(blank).contains("Consumer.PhoneNumber"), blank.toString());
    }
  }

  @Test
  void nationalIdMustBeTenDigitsStartingWithOneOrTwo() throws Exception {
    try (MockApi api = new MockApi()) {
      Consumer bad =
          Consumer.builder()
              .firstName("Noura")
              .lastName("Al-Shammari")
              .phoneNumber("+966512349999")
              .nationalId("3012345678")
              .build();
      ValidationException e = reject(api, Fixtures.threeLineBasket("G").consumer(bad).build());
      assertEquals(List.of("Consumer.NationalId"), fields(e));

      api.enqueue(
          201,
          Fixtures.envelope(
              "SESSION_CREATED",
              "{\"installmentId\":\"1\",\"paymentUrl\":\"https://p\",\"status\":\"New\"}"));
      Consumer good =
          Consumer.builder()
              .firstName("Noura")
              .lastName("Al-Shammari")
              .phoneNumber("+966512349999")
              .nationalId("2012345678")
              .build();
      QasimhaClient.builder()
          .apiKey(Fixtures.TEST_KEY)
          .baseUrl(api.baseUrl())
          .build()
          .checkout()
          .createSession(Fixtures.threeLineBasket("G2").consumer(good).build());
      assertEquals(1, api.requestCount());
    }
  }

  @Test
  void posSessionRequiresEmailAndChecksTheBasket() throws Exception {
    try (MockApi api = new MockApi()) {
      QasimhaClient client =
          QasimhaClient.builder().apiKey(Fixtures.TEST_KEY).baseUrl(api.baseUrl()).build();
      CreatePosSessionRequest noEmail =
          CreatePosSessionRequest.builder()
              .orderReferenceId("POS-V")
              .totalAmount(new BigDecimal("250.00"))
              .addItem(Fixtures.item("SKU-P", "Kettle", Item.TYPE_PHYSICAL, 1, "250.00"))
              .consumer(
                  Consumer.builder()
                      .firstName("Noura")
                      .lastName("Al-Shammari")
                      .phoneNumber("+966512349999")
                      .build())
              .build();
      ValidationException e =
          assertThrows(
              ValidationException.class, () -> client.checkout().createPosSession(noEmail));
      assertEquals(List.of("Consumer.Email"), fields(e));

      CreatePosSessionRequest mismatch =
          CreatePosSessionRequest.builder()
              .orderReferenceId("POS-V2")
              .totalAmount(new BigDecimal("300.00"))
              .addItem(Fixtures.item("SKU-P", "Kettle", Item.TYPE_PHYSICAL, 1, "250.00"))
              .consumer(Fixtures.consumer())
              .build();
      ValidationException m =
          assertThrows(
              ValidationException.class, () -> client.checkout().createPosSession(mismatch));
      assertEquals(List.of("TotalAmount"), fields(m));
      assertEquals(0, api.requestCount());
    }
  }

  @Test
  void linkChannelNeedsAPhoneAndAmounts() throws Exception {
    try (MockApi api = new MockApi()) {
      QasimhaClient client =
          QasimhaClient.builder().apiKey(Fixtures.TEST_KEY).baseUrl(api.baseUrl()).build();
      ValidationException e =
          assertThrows(
              ValidationException.class,
              () ->
                  client
                      .checkoutLinks()
                      .create(
                          CreateCheckoutLinkRequest.builder()
                              .amount(new BigDecimal("250.00"))
                              .channel(CheckoutLinkChannel.LINK)
                              .build()));
      assertEquals(List.of("Phone"), fields(e));

      ValidationException decimals =
          assertThrows(
              ValidationException.class,
              () ->
                  client
                      .checkoutLinks()
                      .create(
                          CreateCheckoutLinkRequest.builder()
                              .amount(new BigDecimal("250.001"))
                              .channel(CheckoutLinkChannel.QR)
                              .language("de")
                              .build()));
      assertTrue(fields(decimals).contains("Amount"), decimals.toString());
      assertTrue(fields(decimals).contains("Language"), decimals.toString());
      assertEquals(0, api.requestCount());
    }
  }

  @Test
  void refundAndFulfilAmountsAreChecked() throws Exception {
    try (MockApi api = new MockApi()) {
      QasimhaClient client =
          QasimhaClient.builder().apiKey(Fixtures.TEST_KEY).baseUrl(api.baseUrl()).build();
      assertEquals(
          List.of("Amount"),
          fields(
              assertThrows(
                  ValidationException.class,
                  () -> client.orders().refund("id", RefundRequest.of(BigDecimal.ZERO)))));
      assertEquals(
          List.of("ShippedAmount"),
          fields(
              assertThrows(
                  ValidationException.class,
                  () ->
                      client
                          .orders()
                          .fulfill(
                              "id",
                              FulfillRequest.builder().shippedAmount(BigDecimal.ZERO).build()))));
      assertEquals(0, api.requestCount());
    }
  }
}
