package com.qasimha.sdk;

/**
 * Pauses the calling thread between retry attempts; replace it in tests to make retries instant.
 */
@FunctionalInterface
public interface Sleeper {

  /** Blocks for the given number of milliseconds. */
  void sleep(long millis) throws InterruptedException;

  /** The default sleeper, backed by {@link Thread#sleep(long)}. */
  static Sleeper system() {
    return Thread::sleep;
  }
}
