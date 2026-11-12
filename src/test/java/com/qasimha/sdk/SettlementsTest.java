package com.qasimha.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qasimha.sdk.exception.AuthenticationException;
import com.qasimha.sdk.exception.PermissionException;
import com.qasimha.sdk.model.Page;
import com.qasimha.sdk.model.PageRequest;
import com.qasimha.sdk.model.Settlement;
import com.qasimha.sdk.model.SettlementOverview;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SettlementsTest {

  private static QasimhaClient client(MockApi api) {
    return QasimhaClient.builder().apiKey(Fixtures.TEST_KEY).baseUrl(api.baseUrl()).build();
  }

  @Test
  void listAndOverview() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
              200,
              Fixtures.envelope(
                  "SUCCESS",
                  "{\"data\":[{\"id\":\"s1\",\"providerSettlementId\":\"PS-1\",\"amount\":950.25,\"currencyCode\":\"SAR\","
                      + "\"settledAt\":\"2026-09-20T00:00:00Z\",\"status\":\"Paid\",\"orderCount\":3}],"
                      + "\"pagination\":{\"totalItems\":1,\"page\":1,\"pageSize\":10,\"totalPages\":1}}"))
          .enqueue(
              200,
              Fixtures.envelope(
                  "SUCCESS",
                  "{\"totalSettled\":950.25,\"count\":1,\"settlementCycle\":\"Once a week\",\"settlementModel\":\"Flexible\","
                      + "\"minimumSettlementAmount\":500.00,\"remainingForNextSettlement\":0.00,\"notPaidYet\":120.50,"
                      + "\"lastSettlement\":{\"amount\":950.25}}"));
      Settlements settlements = client(api).settlements();

      Page<Settlement> page = settlements.list(PageRequest.of(1, 10));
      assertEquals("page=1&pageSize=10", api.lastRequest().query);
      assertEquals(1, page.getItems().size());
      Settlement settlement = page.getItems().get(0);
      assertEquals("s1", settlement.getId());
      assertEquals("PS-1", settlement.getProviderSettlementId());
      assertEquals(0, new BigDecimal("950.25").compareTo(settlement.getAmount()));
      assertEquals(Instant.parse("2026-09-20T00:00:00Z"), settlement.getSettledAt());
      assertEquals("Paid", settlement.getStatus());
      assertEquals(3, settlement.getOrderCount());
      assertEquals(1, page.getPagination().getTotalItems());

      SettlementOverview overview = settlements.overview();
      assertEquals("/api/v1/merchant/settlements/overview", api.lastRequest().path);
      assertEquals(0, new BigDecimal("950.25").compareTo(overview.getTotalSettled()));
      assertEquals(1L, overview.getCount());
      assertEquals("Once a week", overview.getSettlementCycle());
      assertEquals("Flexible", overview.getSettlementModel());
      assertEquals(0, new BigDecimal("500.00").compareTo(overview.getMinimumSettlementAmount()));
      assertEquals(
          0, new BigDecimal("120.50").compareTo((BigDecimal) overview.getRaw().get("notPaidYet")));
      assertTrue(overview.getRaw().containsKey("lastSettlement"));
    }
  }

  @Test
  void forbiddenWithoutScopeArrivesWithAnEmptyBody() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(
          403,
          Map.of("WWW-Authenticate", "ApiKey realm=\"merchant-api\"", "X-Request-Id", "req-403"),
          "");
      PermissionException e =
          assertThrows(PermissionException.class, () -> client(api).settlements().overview());
      assertEquals(403, e.getHttpStatus());
      assertNull(e.getCode());
      assertEquals("req-403", e.getRequestId());
      assertTrue(e.getRawEnvelope().isEmpty());
      assertTrue(e.getMessage().contains("403"));
    }
  }

  @Test
  void unauthorizedArrivesWithAnEmptyBody() throws Exception {
    try (MockApi api = new MockApi()) {
      api.enqueue(401, Map.of("WWW-Authenticate", "ApiKey realm=\"merchant-api\""), "");
      AuthenticationException e =
          assertThrows(AuthenticationException.class, () -> client(api).settlements().list());
      assertEquals(401, e.getHttpStatus());
      assertNull(e.getRequestId());
      assertTrue(e.getMessage().contains("API key"));
    }
  }
}
