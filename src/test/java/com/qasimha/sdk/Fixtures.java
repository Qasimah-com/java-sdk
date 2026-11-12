package com.qasimha.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.qasimha.sdk.internal.Json;
import com.qasimha.sdk.model.Address;
import com.qasimha.sdk.model.Consumer;
import com.qasimha.sdk.model.CreateSessionRequest;
import com.qasimha.sdk.model.Item;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/** Recorded API responses and the cart-contract basket shared by the tests. */
final class Fixtures {

  static final String TEST_KEY = "sk_test_AbCdEfGh1234";

  private Fixtures() {}

  static String read(String name) {
    try (InputStream in = Fixtures.class.getResourceAsStream("/fixtures/" + name)) {
      if (in == null) {
        throw new IllegalStateException("missing fixture " + name);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static JsonNode json(String name) {
    return Json.readTree(read(name).getBytes(StandardCharsets.UTF_8));
  }

  static String envelope(String code, String data) {
    return "{\"success\":true,\"uuid\":\"11111111-2222-3333-4444-555555555555\",\"code\":\""
        + code
        + "\",\"message\":\"ok\",\"data\":"
        + data
        + ",\"errors\":null}";
  }

  static String failure(String code, String message, String data, String errors) {
    return "{\"success\":false,\"uuid\":\"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee\",\"code\":\""
        + code
        + "\",\"message\":\""
        + message
        + "\",\"data\":"
        + (data == null ? "null" : data)
        + ",\"errors\":"
        + (errors == null ? "null" : errors)
        + "}";
  }

  static Item item(String ref, String name, String type, int quantity, String unitPrice) {
    return Item.builder()
        .referenceId(ref)
        .name(name)
        .type(type)
        .sku(ref)
        .quantity(quantity)
        .unitPrice(new BigDecimal(unitPrice))
        .build();
  }

  static Consumer consumer() {
    return Consumer.builder()
        .firstName("Noura")
        .lastName("Al-Shammari")
        .phoneNumber("+966512349999")
        .email("cart-shopper@qasimha.test")
        .build();
  }

  static Address shippingAddress() {
    return Address.builder()
        .firstName("Noura")
        .lastName("Al-Shammari")
        .line1("King Fahd Rd")
        .city("Riyadh")
        .region("Riyadh")
        .postalCode("12345")
        .countryCode("SA")
        .phoneNumber("+966512349999")
        .build();
  }

  /** Section A of the cart contract: 899 + 3×45 + 120 = 1154, +173.10 VAT, +25 shipping, −50. */
  static CreateSessionRequest.Builder threeLineBasket(String reference) {
    return CreateSessionRequest.builder()
        .orderReferenceId(reference)
        .totalAmount(new BigDecimal("1302.10"))
        .taxAmount(new BigDecimal("173.10"))
        .shippingAmount(new BigDecimal("25.00"))
        .discountAmount(new BigDecimal("50.00"))
        .description("Cart contract test")
        .addItem(item("SKU-1", "Wireless Headphones", Item.TYPE_PHYSICAL, 1, "899.00"))
        .addItem(item("SKU-2", "USB-C Cable", Item.TYPE_PHYSICAL, 3, "45.00"))
        .addItem(item("SKU-3", "Extended Warranty", Item.TYPE_DIGITAL, 1, "120.00"))
        .consumer(consumer())
        .shippingAddress(shippingAddress());
  }
}
