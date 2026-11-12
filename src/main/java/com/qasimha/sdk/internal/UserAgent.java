package com.qasimha.sdk.internal;

import com.qasimha.sdk.Version;

/** {@code qasimha-sdk-java/<version> (java/<runtime>; <os>) [<suffix>]}. */
public final class UserAgent {

  private UserAgent() {}

  public static String build(String suffix) {
    StringBuilder ua =
        new StringBuilder("qasimha-sdk-java/")
            .append(Version.VERSION)
            .append(" (java/")
            .append(sanitize(System.getProperty("java.version", "unknown")))
            .append("; ")
            .append(sanitize(System.getProperty("os.name", "unknown")))
            .append(')');
    if (suffix != null && !suffix.trim().isEmpty()) {
      ua.append(' ').append(sanitize(suffix.trim()));
    }
    return ua.toString();
  }

  /** Whether the text can be carried in a header without injection or encoding issues. */
  public static boolean isHeaderSafe(String text) {
    return text.chars().allMatch(c -> c >= 32 && c < 127);
  }

  private static String sanitize(String text) {
    StringBuilder out = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      out.append(c >= 32 && c < 127 ? c : '?');
    }
    return out.toString();
  }
}
