package com.qasimha.sdk;

import com.qasimha.sdk.internal.HttpResult;
import com.qasimha.sdk.internal.HttpTransport;
import com.qasimha.sdk.model.Page;
import com.qasimha.sdk.model.PageRequest;
import com.qasimha.sdk.model.Settlement;
import com.qasimha.sdk.model.SettlementOverview;
import java.util.LinkedHashMap;
import java.util.Map;

/** Settlement batches and the settlement overview; both need the {@code settlements:read} scope. */
public final class Settlements {

  private final HttpTransport transport;

  Settlements(HttpTransport transport) {
    this.transport = transport;
  }

  /** The first page of settlements, newest first. */
  public Page<Settlement> list() {
    return list(PageRequest.first());
  }

  /** One page of settlements, newest first. */
  public Page<Settlement> list(PageRequest page) {
    PageRequest p = page == null ? PageRequest.first() : page;
    Map<String, String> query = new LinkedHashMap<>();
    query.put("page", p.getPage() == null ? null : p.getPage().toString());
    query.put("pageSize", p.getPageSize() == null ? null : p.getPageSize().toString());
    HttpResult result = transport.get("/merchant/settlements", query);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readPage(result, Settlement.class);
  }

  /** The settlement headline figures. */
  public SettlementOverview overview() {
    HttpResult result = transport.get("/merchant/settlements/overview", null);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, SettlementOverview.class);
  }
}
