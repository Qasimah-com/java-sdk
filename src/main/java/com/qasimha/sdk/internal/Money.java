package com.qasimha.sdk.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Two-decimal money checks and the integer-halala view used for basket identities. */
public final class Money {

  private Money() {}

  /** Whether the value has at most two decimals, so it can be sent without rounding. */
  public static boolean hasAtMostTwoDecimals(BigDecimal value) {
    return value.stripTrailingZeros().scale() <= 2;
  }

  /**
   * The value in halalas (hundredths); the value must already pass {@link #hasAtMostTwoDecimals}.
   */
  public static long halalas(BigDecimal value) {
    return value.setScale(2, RoundingMode.UNNECESSARY).unscaledValue().longValueExact();
  }

  /** The value with exactly two decimals, for messages. */
  public static String plain(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }
}
