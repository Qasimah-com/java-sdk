package com.qasimha.sdk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/** Captures every log entry for assertions. */
final class RecordingLogger implements Logger {

  static final class Entry {
    final LogLevel level;
    final String message;
    final Map<String, Object> fields;

    Entry(LogLevel level, String message, Map<String, Object> fields) {
      this.level = level;
      this.message = message;
      this.fields = new LinkedHashMap<>(fields);
    }

    @Override
    public String toString() {
      return level + " " + message + " " + fields;
    }
  }

  private final List<Entry> entries = new CopyOnWriteArrayList<>();

  @Override
  public void log(LogLevel level, String message, Map<String, Object> fields) {
    entries.add(new Entry(level, message, fields));
  }

  List<Entry> entries() {
    return new ArrayList<>(entries);
  }

  String dump() {
    StringBuilder sb = new StringBuilder();
    for (Entry entry : entries) {
      sb.append(entry).append('\n');
    }
    return sb.toString();
  }
}
