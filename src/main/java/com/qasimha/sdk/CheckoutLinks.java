package com.qasimha.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.qasimha.sdk.internal.BasketValidator;
import com.qasimha.sdk.internal.HttpResult;
import com.qasimha.sdk.internal.HttpTransport;
import com.qasimha.sdk.model.CheckoutLink;
import com.qasimha.sdk.model.CreateCheckoutLinkRequest;
import java.util.List;

/** Payment links and QR orders for merchants without a website. */
public final class CheckoutLinks {

  private final HttpTransport transport;

  CheckoutLinks(HttpTransport transport) {
    this.transport = transport;
  }

  /** Creates a link (SMS'd to the customer) or a QR order and returns the hosted page URL. */
  public CheckoutLink create(CreateCheckoutLinkRequest request) {
    return create(request, RequestOptions.defaults());
  }

  /** Creates a link (SMS'd to the customer) or a QR order and returns the hosted page URL. */
  public CheckoutLink create(CreateCheckoutLinkRequest request, RequestOptions options) {
    ResourceSupport.requireNonNull(request, "request");
    BasketValidator.validate(request);
    HttpResult result = transport.post("/merchant/checkout-links", request, options);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, CheckoutLink.class);
  }

  /** The fifty most recent links, each with a {@code waiting} flag. */
  public List<CheckoutLink> list() {
    HttpResult result = transport.get("/merchant/checkout-links", null);
    ResourceSupport.ensureSuccess(transport, result);
    JsonNode data = ResourceSupport.data(result);
    JsonNode rows = null;
    if (data != null && data.isObject()) {
      rows = data.has("items") ? data.get("items") : data.get("data");
    } else if (data != null && data.isArray()) {
      rows = data;
    }
    return ResourceSupport.readList(rows, CheckoutLink.class, result);
  }

  /** One link's status. */
  public CheckoutLink get(String id) {
    HttpResult result =
        transport.get("/merchant/checkout-links/" + ResourceSupport.requireId(id, "id"), null);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, CheckoutLink.class);
  }

  /** Cancels a link that is still open; {@code 409 NOT_OPEN} once it is not. */
  public CheckoutLink cancel(String id) {
    return cancel(id, RequestOptions.defaults());
  }

  /** Cancels a link that is still open; {@code 409 NOT_OPEN} once it is not. */
  public CheckoutLink cancel(String id, RequestOptions options) {
    HttpResult result =
        transport.post(
            "/merchant/checkout-links/" + ResourceSupport.requireId(id, "id") + "/cancel",
            null,
            options);
    ResourceSupport.ensureSuccess(transport, result);
    return ResourceSupport.readData(result, CheckoutLink.class);
  }
}
