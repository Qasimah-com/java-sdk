package com.qasimha.sdk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Records the requested pauses instead of sleeping, so retry tests finish instantly. */
final class RecordingSleeper implements Sleeper {

  private final List<Long> delays = new CopyOnWriteArrayList<>();

  @Override
  public void sleep(long millis) {
    delays.add(millis);
  }

  List<Long> delays() {
    return new ArrayList<>(delays);
  }
}
