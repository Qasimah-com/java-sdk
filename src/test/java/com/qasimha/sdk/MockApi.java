package com.qasimha.sdk;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/** An in-process API that answers scripted responses in order and records every request. */
final class MockApi implements AutoCloseable {

  static final class Recorded {
    final String method;
    final String path;
    final String rawPath;
    final String query;
    final Map<String, List<String>> headers;
    final byte[] body;

    Recorded(
        String method,
        String path,
        String rawPath,
        String query,
        Map<String, List<String>> headers,
        byte[] body) {
      this.method = method;
      this.path = path;
      this.rawPath = rawPath;
      this.query = query;
      this.headers = headers;
      this.body = body;
    }

    String header(String name) {
      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        if (entry.getKey().equalsIgnoreCase(name)) {
          return entry.getValue().isEmpty() ? null : entry.getValue().get(0);
        }
      }
      return null;
    }

    String bodyText() {
      return new String(body, StandardCharsets.UTF_8);
    }
  }

  static final class Scripted {
    final int status;
    final Map<String, String> headers;
    final byte[] body;

    Scripted(int status, Map<String, String> headers, byte[] body) {
      this.status = status;
      this.headers = headers;
      this.body = body;
    }
  }

  private final HttpServer server;
  private final ConcurrentLinkedQueue<Scripted> responses = new ConcurrentLinkedQueue<>();
  private final List<Recorded> requests = new CopyOnWriteArrayList<>();

  MockApi() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", this::handle);
    server.start();
  }

  String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  MockApi enqueue(int status, String json) {
    return enqueue(status, Collections.singletonMap("Content-Type", "application/json"), json);
  }

  MockApi enqueue(int status, Map<String, String> headers, String body) {
    responses.add(
        new Scripted(
            status, headers, body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8)));
    return this;
  }

  MockApi enqueueBytes(int status, Map<String, String> headers, byte[] body) {
    responses.add(new Scripted(status, headers, body));
    return this;
  }

  List<Recorded> requests() {
    return new ArrayList<>(requests);
  }

  Recorded lastRequest() {
    return requests.get(requests.size() - 1);
  }

  int requestCount() {
    return requests.size();
  }

  private void handle(HttpExchange exchange) throws IOException {
    byte[] body;
    try (InputStream in = exchange.getRequestBody()) {
      body = in.readAllBytes();
    }
    Map<String, List<String>> headers = new LinkedHashMap<>();
    exchange.getRequestHeaders().forEach((k, v) -> headers.put(k, new ArrayList<>(v)));
    requests.add(
        new Recorded(
            exchange.getRequestMethod(),
            exchange.getRequestURI().getPath(),
            exchange.getRequestURI().getRawPath(),
            exchange.getRequestURI().getRawQuery(),
            headers,
            body));
    Scripted scripted = responses.poll();
    if (scripted == null) {
      byte[] message =
          "{\"success\":false,\"code\":\"NO_SCRIPTED_RESPONSE\"}".getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(599, message.length);
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(message);
      }
      return;
    }
    scripted.headers.forEach((k, v) -> exchange.getResponseHeaders().add(k, v));
    exchange.sendResponseHeaders(
        scripted.status, scripted.body.length == 0 ? -1 : scripted.body.length);
    if (scripted.body.length > 0) {
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(scripted.body);
      }
    } else {
      exchange.close();
    }
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
