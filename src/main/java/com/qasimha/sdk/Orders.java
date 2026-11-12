package com.qasimha.sdk;

import com.qasimha.sdk.exception.ConfigurationException;
import com.qasimha.sdk.internal.BasketValidator;
import com.qasimha.sdk.internal.HttpResult;
import com.qasimha.sdk.internal.HttpTransport;
import com.qasimha.sdk.model.CancelResult;
import com.qasimha.sdk.model.CaptureResult;
import com.qasimha.sdk.model.ContractDocument;
import com.qasimha.sdk.model.FulfillRequest;
import com.qasimha.sdk.model.FulfillResult;
import com.qasimha.sdk.model.ListOrdersParams;
import com.qasimha.sdk.model.Order;
import com.qasimha.sdk.model.OrderCounts;
import com.qasimha.sdk.model.OrderSummary;
import com.qasimha.sdk.model.Page;
import com.qasimha.sdk.model.RefundRequest;
import com.qasimha.sdk.model.RefundResult;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and manages orders, by Qasimha id or by your own reference. An order that completed the
 * hosted checkout is already {@code Confirmed}; {@link #capture} applies to {@code New}/{@code
 * Approved} orders only, {@link #fulfill} on shipment and {@link #refund} after that.
 */
public final class Orders {

  private static final Pattern FILENAME_STAR =
      Pattern.compile("filename\\*=(?:UTF-8|utf-8)''([^;]+)");
  private static final Pattern FILENAME = Pattern.compile("filename=\"?([^\";]+)\"?");

  private final HttpTransport transport;

  Orders(HttpTransport transport) {
    this.transport = transport;
  }

  /** The first page of every order, newest first. */
  public Page<OrderSummary> list() {
    return list(ListOrdersParams.all());
  }

  /** Lists orders filtered by status and/or search text, paginated. */
  public Page<OrderSummary> list(ListOrdersParams params) {
    ListOrdersParams p = params == null ? ListOrdersParams.all() : params;
    Map<String, String> query = new LinkedHashMap<>();
    query.put("status", p.getStatus());
    query.put("search", p.getSearch());
    query.put("page", p.getPage() == null ? null : p.getPage().toString());
    query.put("pageSize", p.getPageSize() == null ? null : p.getPageSize().toString());
    HttpResult result = transport.get("/merchant/orders", query);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readPage(result, OrderSummary.class);
  }

  /** One order in full, by its Qasimha id. */
  public Order get(String id) {
    HttpResult result =
        transport.get("/merchant/orders/" + ResourceSupport.requireId(id, "id"), null);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, Order.class);
  }

  /**
   * One order in full, by the {@code orderReferenceId} you sent when it was created: for a webhook
   * or a return that names your reference, or a store that never stored the Qasimha id. The match
   * is exact and case-sensitive; a slash in the reference is kept as a path separator. Another
   * merchant's reference or an unknown one is a {@link
   * com.qasimha.sdk.exception.NotFoundException}.
   */
  public Order getByReference(String orderReferenceId) {
    if (orderReferenceId == null || orderReferenceId.trim().isEmpty()) {
      throw new ConfigurationException("orderReferenceId must not be blank");
    }
    HttpResult result =
        transport.get(
            "/merchant/orders/by-reference/" + HttpTransport.remainder(orderReferenceId.trim()),
            null,
            "/merchant/orders/by-reference/{orderReferenceId}");
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, Order.class);
  }

  /** Confirms (captures) a {@code New}/{@code Approved} order. */
  public CaptureResult capture(String id) {
    return capture(id, RequestOptions.defaults());
  }

  /** Confirms (captures) a {@code New}/{@code Approved} order. */
  public CaptureResult capture(String id, RequestOptions options) {
    HttpResult result =
        transport.post(
            "/merchant/orders/" + ResourceSupport.requireId(id, "id") + "/capture", null, options);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, CaptureResult.class);
  }

  /** Cancels (voids) an order that has not been fulfilled. */
  public CancelResult cancel(String id) {
    return cancel(id, RequestOptions.defaults());
  }

  /** Cancels (voids) an order that has not been fulfilled. */
  public CancelResult cancel(String id, RequestOptions options) {
    HttpResult result =
        transport.post(
            "/merchant/orders/" + ResourceSupport.requireId(id, "id") + "/cancel", null, options);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, CancelResult.class);
  }

  /** Refunds a fulfilled order, fully or partially. */
  public RefundResult refund(String id, RefundRequest request) {
    return refund(id, request, RequestOptions.defaults());
  }

  /** Refunds a fulfilled order, fully or partially. */
  public RefundResult refund(String id, RefundRequest request, RequestOptions options) {
    ResourceSupport.requireNonNull(request, "request");
    BasketValidator.validate(request);
    HttpResult result =
        transport.post(
            "/merchant/orders/" + ResourceSupport.requireId(id, "id") + "/refund",
            request,
            options);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, RefundResult.class);
  }

  /** Records a full or partial fulfilment (shipment). */
  public FulfillResult fulfill(String id, FulfillRequest request) {
    return fulfill(id, request, RequestOptions.defaults());
  }

  /** Records a full or partial fulfilment (shipment). */
  public FulfillResult fulfill(String id, FulfillRequest request, RequestOptions options) {
    FulfillRequest body = request == null ? FulfillRequest.all() : request;
    BasketValidator.validate(body);
    HttpResult result =
        transport.post(
            "/merchant/orders/" + ResourceSupport.requireId(id, "id") + "/fulfill", body, options);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, FulfillResult.class);
  }

  /** Per-tab order counts. */
  public OrderCounts counts() {
    HttpResult result = transport.get("/merchant/orders/counts", null);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, OrderCounts.class);
  }

  /** Downloads the order's financing contract (normally a PDF). */
  public ContractDocument contract(String id) {
    HttpResult result =
        transport.getDocument(
            "/merchant/orders/" + ResourceSupport.requireId(id, "id") + "/contract");
    ResourceSupport.ensureSuccess(transport, result);
    String contentType = result.header("Content-Type").orElse(null);
    String fileName = fileName(result.header("Content-Disposition").orElse(null));
    return new ContractDocument(result.getBody(), contentType, fileName, result.getRequestId());
  }

  private static String fileName(String contentDisposition) {
    if (contentDisposition == null) {
      return null;
    }
    Matcher star = FILENAME_STAR.matcher(contentDisposition);
    if (star.find()) {
      return java.net.URLDecoder.decode(
          star.group(1).trim(), java.nio.charset.StandardCharsets.UTF_8);
    }
    Matcher plain = FILENAME.matcher(contentDisposition);
    return plain.find() ? plain.group(1).trim() : null;
  }
}
