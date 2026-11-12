package com.qasimha.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qasimha.sdk.exception.ConfigurationException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class QasimhaClientBuilderTest {

  @Test
  void infersSandboxFromTestKey() {
    QasimhaClient client = QasimhaClient.builder().apiKey("sk_test_abc123").build();
    assertEquals(Environment.SANDBOX, client.getEnvironment());
    assertEquals("https://api-uat.qasimha.com", client.getBaseUrl());
  }

  @Test
  void infersProductionFromLiveKey() {
    QasimhaClient client = QasimhaClient.builder().apiKey("sk_live_abc123").build();
    assertEquals(Environment.PRODUCTION, client.getEnvironment());
    assertEquals("https://api.qasimha.com", client.getBaseUrl());
  }

  @Test
  void rejectsMissingOrMalformedKey() {
    assertThrows(ConfigurationException.class, () -> QasimhaClient.builder().build());
    assertThrows(
        ConfigurationException.class, () -> QasimhaClient.builder().apiKey("pk_test_abc").build());
    assertThrows(
        ConfigurationException.class, () -> QasimhaClient.builder().apiKey("sk_test_").build());
    assertThrows(
        ConfigurationException.class,
        () -> QasimhaClient.builder().apiKey("sk_test_abc-123").build());
  }

  @Test
  void rejectsEnvironmentContradictingKeyPrefix() {
    assertThrows(
        ConfigurationException.class,
        () ->
            QasimhaClient.builder().apiKey("sk_live_abc").environment(Environment.SANDBOX).build());
    assertThrows(
        ConfigurationException.class,
        () ->
            QasimhaClient.builder()
                .apiKey("sk_test_abc")
                .environment(Environment.PRODUCTION)
                .build());
    QasimhaClient ok =
        QasimhaClient.builder().apiKey("sk_test_abc").environment(Environment.SANDBOX).build();
    assertEquals(Environment.SANDBOX, ok.getEnvironment());
  }

  @Test
  void baseUrlOverrideSkipsEnvironmentCheckButRequiresTls() {
    QasimhaClient client =
        QasimhaClient.builder()
            .apiKey("sk_live_abc")
            .environment(Environment.SANDBOX)
            .baseUrl("https://api.example.test/")
            .build();
    assertEquals("https://api.example.test", client.getBaseUrl());
    assertThrows(
        ConfigurationException.class,
        () ->
            QasimhaClient.builder()
                .apiKey("sk_test_abc")
                .baseUrl("http://api.example.test")
                .build());
    assertThrows(
        ConfigurationException.class,
        () -> QasimhaClient.builder().apiKey("sk_test_abc").baseUrl("not a url").build());
    assertThrows(
        ConfigurationException.class,
        () -> QasimhaClient.builder().apiKey("sk_test_abc").baseUrl("https://h/?x=1").build());
  }

  @Test
  void plainHttpIsAllowedForLoopbackOnly() {
    assertEquals(
        "http://localhost:8080",
        QasimhaClient.builder()
            .apiKey("sk_test_abc")
            .baseUrl("http://localhost:8080")
            .build()
            .getBaseUrl());
    assertEquals(
        "http://127.0.0.1:8080",
        QasimhaClient.builder()
            .apiKey("sk_test_abc")
            .baseUrl("http://127.0.0.1:8080/")
            .build()
            .getBaseUrl());
  }

  @Test
  void rejectsBadTimeoutRetriesLanguageAndSuffix() {
    assertThrows(
        ConfigurationException.class,
        () -> QasimhaClient.builder().apiKey("sk_test_abc").timeout(Duration.ZERO).build());
    assertThrows(
        ConfigurationException.class,
        () ->
            QasimhaClient.builder().apiKey("sk_test_abc").timeout(Duration.ofSeconds(-1)).build());
    assertThrows(
        ConfigurationException.class,
        () -> QasimhaClient.builder().apiKey("sk_test_abc").maxRetries(-1).build());
    assertThrows(
        ConfigurationException.class,
        () -> QasimhaClient.builder().apiKey("sk_test_abc").maxRetries(6).build());
    assertThrows(
        ConfigurationException.class,
        () -> QasimhaClient.builder().apiKey("sk_test_abc").language("fr").build());
    assertThrows(
        ConfigurationException.class,
        () ->
            QasimhaClient.builder().apiKey("sk_test_abc").userAgentSuffix("bad\r\nheader").build());
    assertThrows(
        ConfigurationException.class,
        () -> QasimhaClient.builder().apiKey("sk_test_abc").webhookSecret("  ").build());
    QasimhaClient ok =
        QasimhaClient.builder().apiKey("sk_test_abc").maxRetries(5).language("ar").build();
    assertEquals(Environment.SANDBOX, ok.getEnvironment());
  }

  @Test
  void masksTheKeyEverywhere() {
    QasimhaClient client = QasimhaClient.builder().apiKey("sk_live_AbCdEfGh1234WxYz").build();
    assertEquals("sk_live_…WxYz", client.maskedKey());
    assertTrue(client.toString().contains("sk_live_…WxYz"));
    assertFalse(client.toString().contains("AbCdEfGh1234WxYz"));
    assertEquals("sk_test_…ab", QasimhaClient.maskKey("sk_test_ab"));
  }

  @Test
  void publishableKeyMustBeWellFormedAndInTheApiKeyEnvironment() {
    assertThrows(
        ConfigurationException.class,
        () -> QasimhaClient.builder().apiKey("sk_test_abc").publishableKey("pk_live_abc").build());
    assertThrows(
        ConfigurationException.class,
        () -> QasimhaClient.builder().apiKey("sk_live_abc").publishableKey("pk_test_abc").build());
    assertThrows(
        ConfigurationException.class,
        () ->
            QasimhaClient.builder()
                .apiKey("sk_test_abc")
                .baseUrl("https://api.example.test")
                .publishableKey("pk_live_abc")
                .build());
    assertThrows(
        ConfigurationException.class,
        () -> QasimhaClient.builder().apiKey("sk_test_abc").publishableKey("sk_test_abc").build());
    assertThrows(
        ConfigurationException.class,
        () -> QasimhaClient.builder().apiKey("sk_test_abc").publishableKey("pk_test_").build());
    assertThrows(
        ConfigurationException.class,
        () -> QasimhaClient.builder().apiKey("sk_test_abc").publishableKey("pk_test_a-b").build());
    assertThrows(
        ConfigurationException.class,
        () -> QasimhaClient.builder().apiKey("sk_test_abc").publishableKey("").build());
    QasimhaClient sandbox =
        QasimhaClient.builder()
            .apiKey("sk_test_abc")
            .publishableKey("pk_test_AbCdEfGh1234")
            .build();
    assertEquals(Environment.SANDBOX, sandbox.getEnvironment());
    QasimhaClient production =
        QasimhaClient.builder()
            .apiKey("sk_live_abc")
            .publishableKey("pk_live_AbCdEfGh1234")
            .build();
    assertEquals(Environment.PRODUCTION, production.getEnvironment());
  }

  @Test
  void masksThePublishableKeyEverywhere() {
    QasimhaClient client =
        QasimhaClient.builder()
            .apiKey("sk_live_AbCdEfGh1234WxYz")
            .publishableKey("pk_live_QrStUvWx5678MnOp")
            .build();
    assertEquals("pk_live_…MnOp", client.maskedPublishableKey());
    assertTrue(client.toString().contains("publishableKey=pk_live_…MnOp"), client.toString());
    assertFalse(client.toString().contains("QrStUvWx5678MnOp"), client.toString());
    assertEquals("pk_test_…ab", QasimhaClient.maskKey("pk_test_ab"));

    QasimhaClient without = QasimhaClient.builder().apiKey("sk_live_AbCdEfGh1234WxYz").build();
    assertNull(without.maskedPublishableKey());
    assertFalse(without.toString().contains("publishableKey"), without.toString());
  }

  @Test
  void invalidRequestOptions() {
    assertThrows(ConfigurationException.class, () -> RequestOptions.withIdempotencyKey(" "));
    assertThrows(ConfigurationException.class, () -> RequestOptions.withIdempotencyKey("bad key"));
    assertEquals("k-1", RequestOptions.withIdempotencyKey("k-1").getIdempotencyKey());
    assertTrue(RequestOptions.withoutIdempotency().isIdempotencyDisabled());
  }
}
