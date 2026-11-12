package com.qasimha.sdk.internal;

/** Lowercase hex encoding as used by webhook signatures. */
public final class Hex {

  private static final char[] DIGITS = "0123456789abcdef".toCharArray();

  private Hex() {}

  public static String encode(byte[] bytes) {
    char[] out = new char[bytes.length * 2];
    for (int i = 0; i < bytes.length; i++) {
      int v = bytes[i] & 0xFF;
      out[i * 2] = DIGITS[v >>> 4];
      out[i * 2 + 1] = DIGITS[v & 0x0F];
    }
    return new String(out);
  }

  /** Decodes hex of either case; returns null when the text is not valid hex. */
  public static byte[] decode(String hex) {
    if (hex == null || hex.length() % 2 != 0) {
      return null;
    }
    byte[] out = new byte[hex.length() / 2];
    for (int i = 0; i < out.length; i++) {
      int hi = Character.digit(hex.charAt(i * 2), 16);
      int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
      if (hi < 0 || lo < 0) {
        return null;
      }
      out[i] = (byte) ((hi << 4) | lo);
    }
    return out;
  }
}
