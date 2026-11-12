package com.qasimha.sdk;

import java.util.Map;

/**
 * Optional logging callback. The SDK never logs the API key, the webhook secret or consumer data;
 * the fields are {@code method}, {@code path}, {@code status}, {@code durationMs}, {@code
 * requestId}, {@code attempt}, {@code code} and, when a retry is scheduled, {@code delayMs}.
 */
@FunctionalInterface
public interface Logger {

  /** Receives one log entry. */
  void log(LogLevel level, String message, Map<String, Object> fields);
}
