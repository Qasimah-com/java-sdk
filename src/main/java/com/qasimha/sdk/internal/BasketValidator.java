package com.qasimha.sdk.internal;

import com.qasimha.sdk.exception.ValidationException;
import com.qasimha.sdk.model.Address;
import com.qasimha.sdk.model.CheckoutLinkChannel;
import com.qasimha.sdk.model.CheckoutLinkItem;
import com.qasimha.sdk.model.CheckoutPaymentType;
import com.qasimha.sdk.model.Consumer;
import com.qasimha.sdk.model.CreateCheckoutLinkRequest;
import com.qasimha.sdk.model.CreatePosSessionRequest;
import com.qasimha.sdk.model.CreateQrSessionRequest;
import com.qasimha.sdk.model.CreateSessionRequest;
import com.qasimha.sdk.model.FieldError;
import com.qasimha.sdk.model.FulfillRequest;
import com.qasimha.sdk.model.Item;
import com.qasimha.sdk.model.RefundRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Client-side checks that name the failing field before a request is sent. The API remains the
 * authority; these mirror its validator so a plugin gets the failing line locally.
 */
public final class BasketValidator {

  private static final Pattern NATIONAL_ID = Pattern.compile("^[12][0-9]{9}$");

  private BasketValidator() {}

  public static void validate(CreateSessionRequest request) {
    List<FieldError> errors = new ArrayList<>();
    requireText(errors, "OrderReferenceId", request.getOrderReferenceId(), 255);
    boolean amountsOk = amount(errors, "TotalAmount", request.getTotalAmount(), true, false);
    amountsOk &= amount(errors, "TaxAmount", request.getTaxAmount(), false, true);
    amountsOk &= amount(errors, "ShippingAmount", request.getShippingAmount(), false, true);
    amountsOk &= amount(errors, "DiscountAmount", request.getDiscountAmount(), false, true);
    requireLength(errors, "CurrencyCode", request.getCurrencyCode(), 3);
    requireLength(errors, "CountryCode", request.getCountryCode(), 2);
    if (request.getPaymentType() == null
        || CheckoutPaymentType.fromValue(request.getPaymentType()) == null) {
      errors.add(
          new FieldError(
              "PaymentType",
              "Must be one of pay_by_instalments, pay_now, pay_next_month, pay_in_full"));
    }
    if (request.getInstalments() == null || request.getInstalments() < 1) {
      errors.add(new FieldError("Instalments", "Must be at least 1"));
    }
    boolean itemsOk = items(errors, request.getItems());
    consumer(errors, request.getConsumer(), false);
    if (request.getShippingAddress() == null) {
      errors.add(new FieldError("ShippingAddress", "Shipping address is required"));
    } else {
      address(errors, "ShippingAddress", request.getShippingAddress());
    }
    if (request.getBillingAddress() != null) {
      address(errors, "BillingAddress", request.getBillingAddress());
    }
    if (request.getConsumer() != null
        && request.getConsumer().getNationalId() != null
        && !NATIONAL_ID.matcher(request.getConsumer().getNationalId()).matches()) {
      errors.add(new FieldError("Consumer.NationalId", "Must be 10 digits starting with 1 or 2"));
    }
    if (amountsOk && itemsOk) {
      cartIdentity(
          errors,
          request.getTotalAmount(),
          request.getItems(),
          request.getTaxAmount(),
          request.getShippingAmount(),
          request.getDiscountAmount());
    }
    raise(errors);
  }

  public static void validate(CreatePosSessionRequest request) {
    List<FieldError> errors = new ArrayList<>();
    requireText(errors, "OrderReferenceId", request.getOrderReferenceId(), 255);
    boolean amountsOk = amount(errors, "TotalAmount", request.getTotalAmount(), true, false);
    requireLength(errors, "CurrencyCode", request.getCurrencyCode(), 3);
    requireLength(errors, "CountryCode", request.getCountryCode(), 2);
    boolean itemsOk = items(errors, request.getItems());
    consumer(errors, request.getConsumer(), true);
    if (request.getConsumer() != null
        && request.getConsumer().getNationalId() != null
        && !NATIONAL_ID.matcher(request.getConsumer().getNationalId()).matches()) {
      errors.add(new FieldError("Consumer.NationalId", "Must be 10 digits starting with 1 or 2"));
    }
    if (amountsOk && itemsOk) {
      cartIdentity(
          errors,
          request.getTotalAmount(),
          request.getItems(),
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          BigDecimal.ZERO);
    }
    raise(errors);
  }

  public static void validate(CreateQrSessionRequest request) {
    List<FieldError> errors = new ArrayList<>();
    requireText(errors, "OrderReferenceId", request.getOrderReferenceId(), 255);
    boolean amountsOk = amount(errors, "TotalAmount", request.getTotalAmount(), true, false);
    requireLength(errors, "CurrencyCode", request.getCurrencyCode(), 3);
    requireLength(errors, "CountryCode", request.getCountryCode(), 2);
    boolean itemsOk = items(errors, request.getItems());
    if (amountsOk && itemsOk) {
      cartIdentity(
          errors,
          request.getTotalAmount(),
          request.getItems(),
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          BigDecimal.ZERO);
    }
    raise(errors);
  }

  public static void validate(CreateCheckoutLinkRequest request) {
    List<FieldError> errors = new ArrayList<>();
    boolean amountOk = amount(errors, "Amount", request.getAmount(), true, true);
    boolean hasItems = request.getItems() != null && !request.getItems().isEmpty();
    if (amountOk && request.getAmount().signum() == 0 && !hasItems) {
      errors.add(new FieldError("Amount", "Must be greater than zero, or provide items"));
    }
    if (request.getChannel() == null) {
      errors.add(new FieldError("Channel", "Must be Link or Qr"));
    } else if (request.getChannel() == CheckoutLinkChannel.LINK && isBlank(request.getPhone())) {
      errors.add(new FieldError("Phone", "Customer phone number is required for a payment link"));
    }
    if (request.getLanguage() != null
        && !request.getLanguage().equals("ar")
        && !request.getLanguage().equals("en")) {
      errors.add(new FieldError("Language", "Must be ar or en"));
    }
    if (request.getOrderReferenceId() != null && request.getOrderReferenceId().length() > 255) {
      errors.add(new FieldError("OrderReferenceId", "Must be at most 255 characters"));
    }
    if (hasItems) {
      List<CheckoutLinkItem> items = request.getItems();
      for (int i = 0; i < items.size(); i++) {
        CheckoutLinkItem item = items.get(i);
        String prefix = "Items[" + i + "].";
        if (item == null) {
          errors.add(new FieldError("Items[" + i + "]", "Item is required"));
          continue;
        }
        requireText(errors, prefix + "Name", item.getName(), 0);
        if (item.getQuantity() == null || item.getQuantity() < 1) {
          errors.add(new FieldError(prefix + "Quantity", "Must be at least 1"));
        }
        amount(errors, prefix + "UnitPrice", item.getUnitPrice(), true, true);
      }
    }
    raise(errors);
  }

  public static void validate(RefundRequest request) {
    List<FieldError> errors = new ArrayList<>();
    amount(errors, "Amount", request.getAmount(), true, false);
    raise(errors);
  }

  public static void validate(FulfillRequest request) {
    List<FieldError> errors = new ArrayList<>();
    if (request.getShippedAmount() != null) {
      amount(errors, "ShippedAmount", request.getShippedAmount(), false, false);
    }
    raise(errors);
  }

  private static boolean items(List<FieldError> errors, List<Item> items) {
    if (items == null || items.isEmpty()) {
      errors.add(new FieldError("Items", "At least one item is required"));
      return false;
    }
    boolean ok = true;
    for (int i = 0; i < items.size(); i++) {
      Item item = items.get(i);
      String prefix = "Items[" + i + "].";
      if (item == null) {
        errors.add(new FieldError("Items[" + i + "]", "Item is required"));
        ok = false;
        continue;
      }
      requireText(errors, prefix + "ReferenceId", item.getReferenceId(), 0);
      requireText(errors, prefix + "Name", item.getName(), 0);
      requireText(errors, prefix + "Type", item.getType(), 0);
      boolean quantityOk = item.getQuantity() != null && item.getQuantity() >= 1;
      if (!quantityOk) {
        errors.add(new FieldError(prefix + "Quantity", "Must be at least 1"));
      }
      boolean lineOk = amount(errors, prefix + "UnitPrice", item.getUnitPrice(), true, true);
      lineOk &= amount(errors, prefix + "TotalAmount", item.getTotalAmount(), true, true);
      lineOk &= amount(errors, prefix + "TaxAmount", item.getTaxAmount(), false, true);
      lineOk &= amount(errors, prefix + "DiscountAmount", item.getDiscountAmount(), false, true);
      if (quantityOk && lineOk) {
        long expected =
            Money.halalas(item.getUnitPrice()) * item.getQuantity()
                - Money.halalas(zeroIfNull(item.getDiscountAmount()));
        long actual = Money.halalas(item.getTotalAmount());
        if (expected != actual) {
          errors.add(
              new FieldError(
                  prefix + "TotalAmount",
                  "Must equal quantity × unitPrice − discountAmount ("
                      + Money.plain(BigDecimal.valueOf(expected, 2))
                      + "), got "
                      + Money.plain(item.getTotalAmount())));
          ok = false;
        }
      } else {
        ok = false;
      }
    }
    return ok;
  }

  private static void cartIdentity(
      List<FieldError> errors,
      BigDecimal total,
      List<Item> items,
      BigDecimal tax,
      BigDecimal shipping,
      BigDecimal discount) {
    long expected = 0;
    for (Item item : items) {
      expected += Money.halalas(item.getTotalAmount());
    }
    expected += Money.halalas(zeroIfNull(tax));
    expected += Money.halalas(zeroIfNull(shipping));
    expected -= Money.halalas(zeroIfNull(discount));
    long actual = Money.halalas(total);
    if (expected != actual) {
      errors.add(
          new FieldError(
              "TotalAmount",
              "Must equal Σ items.totalAmount + taxAmount + shippingAmount − discountAmount ("
                  + Money.plain(BigDecimal.valueOf(expected, 2))
                  + "), got "
                  + Money.plain(total)));
    }
  }

  private static void consumer(List<FieldError> errors, Consumer consumer, boolean emailRequired) {
    if (consumer == null) {
      errors.add(new FieldError("Consumer", "Consumer information is required"));
      return;
    }
    requireText(errors, "Consumer.FirstName", consumer.getFirstName(), 0);
    requireText(errors, "Consumer.LastName", consumer.getLastName(), 0);
    requireText(errors, "Consumer.PhoneNumber", consumer.getPhoneNumber(), 0);
    if (emailRequired) {
      requireText(errors, "Consumer.Email", consumer.getEmail(), 0);
    }
  }

  private static void address(List<FieldError> errors, String prefix, Address address) {
    requireText(errors, prefix + ".FirstName", address.getFirstName(), 0);
    requireText(errors, prefix + ".LastName", address.getLastName(), 0);
    requireText(errors, prefix + ".Line1", address.getLine1(), 0);
    requireText(errors, prefix + ".City", address.getCity(), 0);
    requireLength(errors, prefix + ".CountryCode", address.getCountryCode(), 2);
  }

  private static boolean amount(
      List<FieldError> errors,
      String field,
      BigDecimal value,
      boolean required,
      boolean allowZero) {
    if (value == null) {
      if (required) {
        errors.add(new FieldError(field, "Amount is required"));
        return false;
      }
      return true;
    }
    if (value.signum() < 0) {
      errors.add(new FieldError(field, "Must not be negative"));
      return false;
    }
    if (!allowZero && value.signum() == 0) {
      errors.add(new FieldError(field, "Must be greater than zero"));
      return false;
    }
    if (!Money.hasAtMostTwoDecimals(value)) {
      errors.add(
          new FieldError(field, "Must have at most two decimals, got " + value.toPlainString()));
      return false;
    }
    return true;
  }

  private static void requireText(List<FieldError> errors, String field, String value, int max) {
    if (isBlank(value)) {
      errors.add(new FieldError(field, "Is required"));
    } else if (max > 0 && value.length() > max) {
      errors.add(new FieldError(field, "Must be at most " + max + " characters"));
    }
  }

  private static void requireLength(
      List<FieldError> errors, String field, String value, int length) {
    if (isBlank(value) || value.length() != length) {
      errors.add(new FieldError(field, "Must be " + length + " characters"));
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private static BigDecimal zeroIfNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static void raise(List<FieldError> errors) {
    if (!errors.isEmpty()) {
      throw ValidationException.clientSide(errors);
    }
  }
}
