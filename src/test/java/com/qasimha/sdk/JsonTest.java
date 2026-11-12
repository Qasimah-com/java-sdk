package com.qasimha.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qasimha.sdk.internal.LenientInstantDeserializer;
import com.qasimha.sdk.model.InstallmentItemStatus;
import com.qasimha.sdk.model.OrderChannel;
import com.qasimha.sdk.model.OrderStatus;
import com.qasimha.sdk.model.PaymentType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JsonTest {

  @Test
  void timestampsWithAndWithoutOffsetsAreRead() {
    assertEquals(
        Instant.parse("2026-09-25T08:18:54.986832Z"),
        LenientInstantDeserializer.parse("2026-09-25T08:18:54.986832Z"));
    assertEquals(
        Instant.parse("2026-10-25T00:00:00Z"),
        LenientInstantDeserializer.parse("2026-10-25T00:00:00"));
    assertEquals(
        Instant.parse("2026-10-25T00:00:00Z"), LenientInstantDeserializer.parse("2026-10-25"));
    assertEquals(
        Instant.parse("2026-09-25T08:00:00Z"),
        LenientInstantDeserializer.parse("2026-09-25T11:00:00+03:00"));
    assertNull(LenientInstantDeserializer.parse("yesterday"));
  }

  @Test
  void statusNamesAreCanonicalisedAndUnknownValuesPassThrough() {
    assertSame(OrderStatus.CONFIRMED, OrderStatus.of("confirmed"));
    assertSame(OrderStatus.CONFIRMED, OrderStatus.of("Confirmed"));
    assertSame(OrderStatus.DECLINED, OrderStatus.of("3"));
    assertSame(OrderStatus.DECLINED, OrderStatus.fromCode(3));
    assertSame(OrderStatus.WRITTEN_OFF, OrderStatus.fromCode(15));
    assertEquals(16, OrderStatus.known().size());
    assertEquals("99", OrderStatus.fromCode(99).getName());
    assertFalse(OrderStatus.fromCode(99).isKnown());
    assertEquals("Foo", OrderStatus.of("Foo").getName());
    assertFalse(OrderStatus.of("Foo").isKnown());
    assertEquals(OrderStatus.of("Foo"), OrderStatus.of("Foo"));
    assertEquals("Confirmed", OrderStatus.CONFIRMED.toString());
    assertEquals(5, OrderStatus.CONFIRMED.getCode());
    assertNull(OrderStatus.of(null));
  }

  @Test
  void otherEnumsBehaveTheSame() {
    assertSame(OrderChannel.IN_STORE, OrderChannel.of("instore"));
    assertSame(OrderChannel.QR_CODE, OrderChannel.fromCode(2));
    assertEquals("9", OrderChannel.fromCode(9).getName());
    assertSame(PaymentType.PAY_NEXT_MONTH, PaymentType.fromCode(2));
    assertSame(PaymentType.PAY_IN_FULL, PaymentType.of("PayInFull"));
    assertTrue(PaymentType.of("Later").getName().equals("Later"));
    assertSame(InstallmentItemStatus.WAIVED, InstallmentItemStatus.fromCode(4));
    assertSame(InstallmentItemStatus.PAID, InstallmentItemStatus.of("paid"));
    assertEquals("7", InstallmentItemStatus.of("7").getName());
  }
}
