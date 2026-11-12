# Qasimha SDK for Java

Server-side Java client for the Qasimha merchant API: quote instalment plans for an amount, create
checkout sessions, manage orders (by Qasimha id or by your own reference), issue payment links and
QR orders, read settlements, manage webhook endpoints and verify webhook deliveries.

- Java 11+ (works on Spring Boot 2.x and 3.x, `javax` and `jakarta` servlet containers)
- One runtime dependency: Jackson (`jackson-databind`, `jackson-datatype-jsr310`)
- HTTP via the JDK's `java.net.http.HttpClient`; no logging framework dependency
- Blocking calls that return typed models; every error is a typed exception

## Install

```sh
cd /path/to/repo/sdks/java              # your copy of this repository
mvn install                             # with Maven 3.6.3 or newer installed
```

Without Maven, use the Maven Wrapper that ships with the demo project; it downloads Maven on first
use:

```sh
cd /path/to/repo/sdks/java/examples/demo-merchant-server
./mvnw -f ../../pom.xml install -DskipTests    # mvnw.cmd on Windows
```

Then depend on it as usual. Maven reads `~/.m2` without further configuration:

```xml
<dependency>
  <groupId>com.qasimha</groupId>
  <artifactId>qasimha-sdk</artifactId>
  <version>0.1.0</version>
</dependency>
```

Gradle needs `mavenLocal()` in its repositories:

```kotlin
repositories {
    mavenLocal()      // the locally installed qasimha-sdk; remove once it is on Maven Central
    mavenCentral()    // Jackson
}

dependencies {
    implementation("com.qasimha:qasimha-sdk:0.1.0")
}
```

Every machine that builds your project (including CI) needs the same `install` step, or the jar
in a Maven repository you host. Once the package is on Maven Central, the same coordinates work
with no `install` step and no `mavenLocal()`.

More on installing every package: [installation.md](../../docs/installation.md).

## Configuration

### Environments

| | Sandbox | Production |
|---|---|---|
| API host | `https://api-uat.qasimha.com` | `https://api.qasimha.com` |
| Secret key | `sk_test_…` | `sk_live_…` |
| Publishable key | `pk_test_…` | `pk_live_…` |

Create keys in the Merchant Portal under Settings → API Keys. The SDK picks the environment from
the key prefix: a `sk_test_` key talks to the sandbox, a `sk_live_` key to production. You do not
set a URL. The `.baseUrl(...)` option exists only for a test server you run yourself; leave it
unset in sandbox and production.

### Where the values live

Keep keys and secrets out of source control and out of the browser or mobile app. Read them from
environment variables (or a secret store that sets them):

| Variable | Value |
|---|---|
| `QASIMHA_API_KEY` | `sk_test_…` in sandbox, `sk_live_…` in production. Required. |
| `QASIMHA_WEBHOOK_SECRET` | `whsec_…` of the webhook endpoint registered in that environment. |
| `QASIMHA_PUBLISHABLE_KEY` | `pk_test_…` / `pk_live_…`. Only if you quote plans with `checkout().getPlans(...)`. |
| `STORE_BASE_URL` (your own name) | The public base URL of this deployment, used to build return URLs. |

```java
import com.qasimha.sdk.QasimhaClient;
import com.qasimha.sdk.model.MerchantUrl;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

QasimhaClient client = QasimhaClient.builder()
    .apiKey(System.getenv("QASIMHA_API_KEY"))
    .webhookSecret(System.getenv("QASIMHA_WEBHOOK_SECRET"))       // null when unset
    .publishableKey(System.getenv("QASIMHA_PUBLISHABLE_KEY"))     // optional
    .build();

String storeBaseUrl = System.getenv("STORE_BASE_URL");             // e.g. https://shop.example
String order = URLEncoder.encode(orderReferenceId, StandardCharsets.UTF_8);
MerchantUrl merchantUrl = MerchantUrl.builder()
    .success(storeBaseUrl + "/checkout/qasimha/return?order=" + order)
    .failure(storeBaseUrl + "/checkout/qasimha/return?order=" + order)
    .cancel(storeBaseUrl + "/cart")
    .build();
```

Spring Boot: reference the environment variables from `application.properties`, so the file
itself holds no secret:

```properties
qasimha.api-key=${QASIMHA_API_KEY}
qasimha.webhook-secret=${QASIMHA_WEBHOOK_SECRET:}
qasimha.publishable-key=${QASIMHA_PUBLISHABLE_KEY:}
store.base-url=${STORE_BASE_URL}
```

```java
@Configuration
public class QasimhaConfig {

  @Bean
  public QasimhaClient qasimhaClient(
      @Value("${qasimha.api-key}") String apiKey,
      @Value("${qasimha.webhook-secret}") String webhookSecret,
      @Value("${qasimha.publishable-key}") String publishableKey) {
    return QasimhaClient.builder()
        .apiKey(apiKey)
        .webhookSecret(webhookSecret.isEmpty() ? null : webhookSecret)       // a blank value is refused
        .publishableKey(publishableKey.isEmpty() ? null : publishableKey)
        .build();
  }
}
```

### Return URLs

`merchantUrl` (`success`, `failure`, optional `cancel`) tells the hosted checkout where to send
the shopper afterwards. Use `https` URLs on your own site (plain `http` only for `localhost`), and
build them from the deployment's base URL so sandbox orders return to your test site and
production orders to your live site. Qasimha appends `orderId`, `orderReferenceId` and `status`
to the URL. The return is not proof of payment: always confirm with `orders().get(orderId)` (see
[Handle the return](#handle-the-return-verify-with-ordersget)). Details:
[return-url-contract.md](../../docs/return-url-contract.md); every setting in one place:
[configuration.md](../../docs/configuration.md).

### Switching to production

1. Replace `QASIMHA_API_KEY` (and `QASIMHA_PUBLISHABLE_KEY`) with the `sk_live_…` / `pk_live_…`
   keys from the production Merchant Portal. No code change.
2. Point `STORE_BASE_URL` at the live site, so the return URLs are production URLs.
3. Register the production webhook URL and set its `whsec_…` secret as `QASIMHA_WEBHOOK_SECRET`.
4. Remove any `.baseUrl(...)` override.

### Client options

```java
QasimhaClient client = QasimhaClient.builder()
    .apiKey(System.getenv("QASIMHA_API_KEY"))            // sk_test_… or sk_live_…
    .webhookSecret(System.getenv("QASIMHA_WEBHOOK_SECRET")) // whsec_…, for client.webhooks()
    .publishableKey(System.getenv("QASIMHA_PUBLISHABLE_KEY")) // pk_test_… or pk_live_…, for getPlans
    .language("en")                                       // "ar" or "en" for API messages
    .userAgentSuffix("my-store/1.4.0")                    // appended to the User-Agent
    .build();
```

The environment is inferred from the key prefix: `sk_test_` keys talk to the sandbox
(`https://api-uat.qasimha.com`), `sk_live_` keys to production (`https://api.qasimha.com`).
Passing `.environment(...)` that contradicts the key is a `ConfigurationException`. A
`.baseUrl(...)` override wins over the environment and must be `https://` (plain `http://` is
accepted for `localhost` and `127.0.0.1` only).

Other options: `.timeout(Duration)` per attempt (default 30 s), `.maxRetries(int)` 0–5 (default 2),
`.logger(...)` (see Logging), `.httpClient(HttpClient)` to supply your own client (build it with
`HttpClient.Redirect.NEVER`). The optional `.publishableKey(...)` is only used by
`checkout().getPlans(...)` and must belong to the API key's environment: `pk_test_` with
`sk_test_`, `pk_live_` with `sk_live_`; a mismatch is a `ConfigurationException`.

One client per API key; it is thread-safe and meant to be shared.

## Create a checkout session and redirect

Amounts are `BigDecimal` with at most two decimals (`new BigDecimal("1302.10")`, never the
`double` constructor). The basket has to add up, and the SDK checks it before sending so the failing
line is named locally:

- `item.totalAmount == quantity × unitPrice − item.discountAmount` (the `Item` builder computes it
  when left unset)
- `totalAmount == Σ item.totalAmount + taxAmount + shippingAmount − discountAmount`

```java
import com.qasimha.sdk.exception.DuplicateOrderReferenceException;
import com.qasimha.sdk.exception.OrderDeclinedException;
import com.qasimha.sdk.model.*;
import java.math.BigDecimal;

CreateSessionRequest request = CreateSessionRequest.builder()
    .orderReferenceId("ORDER-12345")                 // your own id, unique per order
    .totalAmount(new BigDecimal("1302.10"))
    .taxAmount(new BigDecimal("173.10"))
    .shippingAmount(new BigDecimal("25.00"))
    .discountAmount(new BigDecimal("50.00"))
    .description("Order #12345")
    .addItem(Item.builder()
        .referenceId("SKU-1").name("Wireless Headphones").type(Item.TYPE_PHYSICAL)
        .sku("SKU-1").quantity(1).unitPrice(new BigDecimal("899.00")).build())
    .addItem(Item.builder()
        .referenceId("SKU-2").name("USB-C Cable").type(Item.TYPE_PHYSICAL)
        .sku("SKU-2").quantity(3).unitPrice(new BigDecimal("45.00")).build())
    .addItem(Item.builder()
        .referenceId("SKU-3").name("Extended Warranty").type(Item.TYPE_DIGITAL)
        .sku("SKU-3").quantity(1).unitPrice(new BigDecimal("120.00")).build())
    .consumer(Consumer.builder()
        .firstName("Noura").lastName("Al-Shammari")
        .phoneNumber("+9665XXXXXXXX").email("customer@example.com").build())
    .shippingAddress(Address.builder()                // required by the API
        .firstName("Noura").lastName("Al-Shammari")
        .line1("King Fahd Rd").city("Riyadh").countryCode("SA").build())
    .merchantUrl(MerchantUrl.builder()                // see the note below
        .success("https://store.example/checkout/return?order=12345")
        .failure("https://store.example/checkout/failed?order=12345")
        .cancel("https://store.example/cart")
        .build())
    .build();

try {
  Session session = client.checkout().createSession(request);
  saveQasimhaOrderId("ORDER-12345", session.getOrderId());  // needed for every later call
  redirect(session.getPaymentUrl());
} catch (OrderDeclinedException e) {
  // created but not approved: nothing to pay; e.getOrderId() is the declined order
  showDeclined();
} catch (DuplicateOrderReferenceException e) {
  // the reference is already in use; send the shopper back to the checkout already open
  redirect(e.getExistingPaymentUrl());
}
```

`paymentType` defaults to `pay_by_instalments` and `instalments` to 4; `currencyCode` is `SAR`
and `countryCode` is `SA` (the API accepts SAR only).

After the hosted checkout the shopper is sent to the `merchantUrl` `success`, `failure` or
`cancel` URL. Still drive the order state from webhooks and `orders().get(id)`
rather than from the return visit alone.

In-store (`createPosSession`, `consumer.email` required, returns the hosted page `checkoutUrl`) and
scan-to-pay (`createQrSession`, returns `qrCodeUrl`) work the same way. A declined QR order is
raised as `OrderDeclinedException` too, so the three endpoints behave alike.

## Handle the return (verify with `orders().get`)

Never trust a return visit or a webhook on its own. Before marking anything paid, read the order:

```java
Order order = client.orders().get(qasimhaOrderId);
if (OrderStatus.CONFIRMED.equals(order.getStatus())) {
  markPaid(order.getOrderReferenceId());
}
```

An order that completed the hosted checkout is already **`Confirmed`** (its first instalment is
paid). `Order` exposes the curated fields (amounts, timestamps, `customer`, `items`,
`installments`, `fulfillments`, `reversals`, `voids`, `statusHistory`); everything else the API
returns stays reachable through `order.getRaw()`.

`status`, `channel`, `paymentType` and instalment `status` are exposed by name (`OrderStatus`,
`OrderChannel`, `PaymentType`, `InstallmentItemStatus`) whether the API sends the integer or the
name; a value the SDK does not know yet is passed through as received and never throws.

## Look an order up by your reference

A webhook, a return visit or a store that never stored the Qasimha id often only holds your own
`orderReferenceId`. `getByReference` answers the same `Order` as `get`:

```java
import com.qasimha.sdk.exception.NotFoundException;

try {
  Order order = client.orders().getByReference("ORDER-12345");
  if (OrderStatus.CONFIRMED.equals(order.getStatus())) {
    markPaid(order.getOrderReferenceId());
  }
} catch (NotFoundException e) {
  // never created with this key, or created by another merchant
}
```

The match is exact and case-sensitive. A reference may contain `/`: the SDK percent-encodes each
segment and keeps the slashes, so `ORDER/2026/1` is requested as
`/merchant/orders/by-reference/ORDER/2026/1`. The reference is not written to the SDK log; the
path is logged as `/api/v1/merchant/orders/by-reference/{orderReferenceId}`.

## Webhooks (raw body, `constructEvent`)

Qasimha signs every delivery. The SDK verifies both signature formats — `t=<unix>,v1=<hex>` over
`"<t>." + body` (several `v1` during a secret rotation) on an endpoint you register, and
`X-Qasimha-Signature: sha256=<hex>` over the raw body on the account-level URL — with constant-time comparison and a
replay tolerance of 300 s. Any failure is a `WebhookSignatureException` whose `getReason()` is
`MISSING_HEADER`, `MALFORMED_HEADER`, `TIMESTAMP_OUTSIDE_TOLERANCE` or `SIGNATURE_MISMATCH`.

**Hand over the raw request bytes exactly as received.** A body that a framework parsed and
re-serialised will not verify.

Plain Java / Jakarta servlet (`javax.servlet` is identical):

```java
import com.qasimha.sdk.exception.WebhookSignatureException;
import com.qasimha.sdk.model.WebhookEvent;
import jakarta.servlet.http.*;

public class QasimhaWebhookServlet extends HttpServlet {
  private final QasimhaClient client = /* the shared client */;

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    byte[] rawBody = request.getInputStream().readAllBytes();
    Map<String, String> headers = new HashMap<>();
    Collections.list(request.getHeaderNames()).forEach(n -> headers.put(n, request.getHeader(n)));

    WebhookEvent event;
    try {
      event = client.webhooks().constructEvent(rawBody, headers);
    } catch (WebhookSignatureException e) {
      response.setStatus(400);
      return;
    }
    response.setStatus(200);           // acknowledge promptly, process afterwards
    enqueue(event);                    // event.getId() deduplicates redeliveries
  }
}
```

Spring Boot:

```java
@RestController
public class QasimhaWebhookController {
  private final QasimhaClient client;

  public QasimhaWebhookController(QasimhaClient client) {
    this.client = client;
  }

  @PostMapping("/webhooks/qasimha")
  public ResponseEntity<Void> receive(
      @RequestBody String rawBody, @RequestHeader Map<String, String> headers) {
    try {
      WebhookEvent event = client.webhooks().constructEvent(rawBody, headers);
      enqueue(event);
      return ResponseEntity.ok().build();
    } catch (WebhookSignatureException e) {
      return ResponseEntity.badRequest().build();
    }
  }
}
```

`@RequestBody String` is the unmodified body as long as no message converter re-serialises it;
if you prefer bytes, take `HttpServletRequest` and read `getInputStream().readAllBytes()`.

The event (`WebhookEvent`) carries `id`, `type`, `orderId`, `orderReferenceId`, `status`,
`amount`, `currency`, `occurredAt` and the whole decoded body in `getRaw()`. Then confirm the
state with `orders().get(event.getOrderId())` or `orders().getByReference(...)` before acting on
it.

An endpoint you register (next section) receives the timestamped signature, retried
deliveries and the versioned body (`id`, `type`, `createdAt`, `orderId`, `orderReferenceId`,
`status`, `amount`, `currency`, `data.order`), with the event types `order.confirmed`,
`order.declined`, `order.fulfilled`, `order.partially_fulfilled`, `order.refunded`,
`order.partially_refunded`, `order.voided`, `order.cancelled`, `order.expired` and
`checkout_link.expired` (`WebhookEventTypes`). An account with no endpoint keeps the account-level URL
set up by Qasimha: `sha256=` signatures and the older body (`eventId`,
`eventType`, `order.completed` / `order.declined` / `order.voided` / `order.reversed`, no order id
— use `orders().getByReference(event.getOrderReferenceId())`). `constructEvent` handles both.

To verify without a client (for example in a service that only receives webhooks):

```java
Webhooks webhooks = new Webhooks(System.getenv("QASIMHA_WEBHOOK_SECRET"));
WebhookEvent event = webhooks.constructEvent(rawBody, headers);
```

## Manage webhook endpoints

`client.webhookEndpoints()` registers where deliveries go and holds the delivery log. Every method
needs a key with the `webhooks:manage` scope (`PermissionException` otherwise); up to five
endpoints per account.

```java
import com.qasimha.sdk.model.*;

WebhookEndpointWithSecret created = client.webhookEndpoints().create(
    CreateWebhookEndpointRequest.builder()
        .url("https://store.example/webhooks/qasimha")   // https only
        .description("Production store")
        .addEvent(WebhookEventTypes.ORDER_CONFIRMED)      // omit events to receive every event
        .addEvent(WebhookEventTypes.ORDER_REFUNDED)
        .build());
storeSecret(created.getSecret());   // returned once; feed it to .webhookSecret(...) on the client

List<WebhookEndpoint> endpoints = client.webhookEndpoints().list();   // secretHint, never the secret
WebhookEndpoint endpoint = client.webhookEndpoints().get(created.getId());
client.webhookEndpoints().update(created.getId(), UpdateWebhookEndpointRequest.active(false));
client.webhookEndpoints().delete(created.getId());
```

The SDK checks the request before sending: `url` must be `https://` and every entry of `events`
must be one of `WebhookEventTypes` (`order.completed` is accepted as an alias of
`order.confirmed`). The API additionally refuses private-network targets (`URL_PRIVATE`) and a
sixth endpoint (`LIMIT_EXCEEDED`), both `ValidationException`.

Rotating the secret and proving the receiver:

```java
WebhookEndpointWithSecret rotated = client.webhookEndpoints().rotateSecret(id);
// until rotated.getPreviousSecretExpiresAt() (24 h) each delivery carries two v1 signatures, one per
// secret, so either secret verifies; switch the receiver to rotated.getSecret() within that window

PingResult ping = client.webhookEndpoints().ping(id);   // a signed "ping" event is queued
Page<WebhookDelivery> log = client.webhookEndpoints().listDeliveries(id,
    ListDeliveriesParams.builder().status(WebhookDelivery.STATUS_FAILED).pageSize(50).build());
for (WebhookDelivery d : log.getItems()) {
  // d.getEventType(), d.getAttempts(), d.getLastStatusCode(), d.getLastError(), d.getNextAttemptAt()
  client.webhookEndpoints().replayDelivery(id, d.getId());   // queued again; replayOfId names the original
}
```

The secret from `create` and `rotateSecret` is exposed by `getSecret()` only: it is never logged
by the SDK and `toString()` omits it.

## Capture / fulfil / refund / cancel

```java
// only for New/Approved orders; a Confirmed order answers 400 INVALID_STATUS
client.orders().capture(orderId);

// on shipment: omit shippedAmount to fulfil everything outstanding
client.orders().fulfill(orderId, FulfillRequest.builder()
    .shippedAmount(new BigDecimal("1200.00"))
    .shippingCompany("Aramex").trackingNumber("ARX123").build());

// after fulfilment; order.getRefundableAmount() is what can still be refunded
client.orders().refund(orderId, RefundRequest.of(new BigDecimal("300.00"), "Customer return"));

// voids an order that has not been fulfilled
client.orders().cancel(orderId);

Page<OrderSummary> page = client.orders().list(ListOrdersParams.builder()
    .status("captured").page(1).pageSize(50).build());
OrderCounts counts = client.orders().counts();
ContractDocument contract = client.orders().contract(orderId);  // the financing contract PDF
```

`status` accepts a status name or the aliases `approved`, `authorised`, `captured`, `refunded`,
`cancelled`; `search` matches your order reference, the customer name, email or phone.

## Payment links & QR

For sales without a website (in-store, WhatsApp, social):

```java
CheckoutLink link = client.checkoutLinks().create(CreateCheckoutLinkRequest.builder()
    .amount(new BigDecimal("250.00"))
    .channel(CheckoutLinkChannel.LINK)     // needs a phone: the customer is SMS'd the link
    .phone("+9665XXXXXXXX")
    .language("ar")
    .orderReferenceId("ORDER-12345")
    .build());
// CheckoutLinkChannel.QR needs no phone: render link.getUrl() as a QR code

List<CheckoutLink> recent = client.checkoutLinks().list();      // the latest 50, with a waiting flag
CheckoutLink status = client.checkoutLinks().get(link.getId()); // completed / declined flags
client.checkoutLinks().cancel(link.getId());                    // 409 NOT_OPEN once it is not open
```

A real-estate merchant passes `.ejarContractNumber(...)`; other merchants leave it unset.

Settlements (`settlements:read` scope): `client.settlements().list()` and
`client.settlements().overview()`.

## Quote instalment plans for an amount

What the on-site widget shows under a price, for a server that renders its own product or cart
pages, or wants to decide whether to offer Qasimha at all before starting a session. The quote is
the merchant's price list for the amount, with no customer data; the checkout quotes the customer's
own terms.

The call authenticates with the **publishable** key (`pk_test_…` / `pk_live_…`, the key that is
safe to embed in a page) and sends it as `X-Public-Key`; the API key is not sent on this request.
Set the key once on the builder, or pass it per call.

```java
import com.qasimha.sdk.GetPlansOptions;
import com.qasimha.sdk.model.Plan;
import com.qasimha.sdk.model.Plans;

Plans quote = client.checkout().getPlans(new BigDecimal("1200.00"));
if (quote.isOffered()) {                       // false outside [minAmount, maxAmount] or with no policy
  for (Plan plan : quote.getPlans()) {
    // plan.getInstalments(), plan.getFrequency(), plan.getInstalmentAmount(),
    // plan.getFirstInstalmentAmount(), plan.getLastInstalmentAmount(),
    // plan.getFeesIncludingVat(), plan.getTotalPayable(), plan.getShariaCompliant()
    render(plan);
  }
}

// Arabic labels, or another key, for this call only
Plans arabic = client.checkout().getPlans(
    new BigDecimal("1200.00"),
    GetPlansOptions.defaults().language("ar").publishableKey(otherPublishableKey));
```

`Plans` carries `merchantName`, `currency` (`SAR`), `minAmount`, `maxAmount` and `plans`; an amount
the policy does not finance answers an empty list, not an exception. The amount must be positive
with at most two decimals (`ValidationException` client-side otherwise, no request sent); without a
publishable key on the client or the call, `getPlans` throws `ConfigurationException` before any
request. A 401 (`PUBLIC_KEY_REQUIRED`, `INVALID_PUBLIC_KEY`) is an `AuthenticationException`, a
400 (`INVALID_AMOUNT`, `UNSUPPORTED_CURRENCY`) a `ValidationException`; the request is retried
like every `GET`. The platform keeps a merchant's quote for an amount for one minute.

## Errors

Every failure is a `QasimhaException` (unchecked) with `getCode()` (the envelope code),
`getHttpStatus()` (`null` for transport failures), `getRequestId()`, `getErrors()` (field errors,
field names as the API sends them: `TotalAmount`, `Items[0].Quantity`) and `getRawEnvelope()`.
Messages come from the API unchanged, localised by `language`. The key never appears in any of it.

| Exception | When |
|---|---|
| `ConfigurationException` | client misconfigured, or an invalid argument before sending |
| `ValidationException` | client-side basket, amount or webhook-endpoint check (`isClientSide()`), or any 400 |
| `AuthenticationException` | 401: missing, invalid or revoked key (API key, or publishable key on `getPlans`) |
| `PermissionException` | 403: the key lacks the scope (`settlements:read`, `webhooks:manage`) |
| `NotFoundException` | 404 |
| `DuplicateOrderReferenceException` | 409 `DUPLICATE_ORDER_REFERENCE`, with `getExistingOrderId()` / `getExistingPaymentUrl()` |
| `ConflictException` | any other 409 (`MERCHANT_SUSPENDED`, `NOT_OPEN`, `REQUEST_IN_PROGRESS`) |
| `OrderDeclinedException` | 422 `ORDER_DECLINED`, with `getOrderId()` |
| `RateLimitException` | 429 after the retries, with `getRetryAfterSeconds()` |
| `ServerException` | 5xx |
| `ApiException` | anything else, including a 3xx or a 2xx with `success: false` |
| `ConnectionException` | DNS, TLS, socket or timeout failure (the cause is attached) |
| `WebhookSignatureException` | a delivery failed verification, with `getReason()` |

## Retries & idempotency

Every `POST`, `PUT` and `DELETE` carries a fresh UUID v4 `Idempotency-Key` and every call an
`X-Correlation-Id`; both are reused across the attempts of one call. The API stores the first response for a key and replays
it (`Idempotent-Replay: true`, exposed as `isReplayed()` on the result) for any retry.

Retried: connection failures, 429, 502, 503, 504, and `409 REQUEST_IN_PROGRESS`. Never retried:
other 4xx, and 500 (a write may already have committed). Backoff is `min(2^attempt × 500 ms, 8 s)`
plus up to 250 ms jitter; a `Retry-After` on 429 is honoured, capped at 30 s. `maxRetries` (0–5,
default 2) is the number of retries after the first attempt.

A `409 DUPLICATE_ORDER_REFERENCE` on the **first** attempt is the double-submit answer and is
raised. On a **retry** it means the earlier attempt succeeded after its response was lost; the SDK
returns it as the `Session` and logs it at `info`.

Per call: `RequestOptions.withIdempotencyKey("your-key")` uses your own key,
`RequestOptions.withoutIdempotency()` sends none and turns retries off for that call.

## Logging

Nothing is logged unless you pass a logger:

```java
QasimhaClient.builder()
    .apiKey(apiKey)
    .logger((level, message, fields) -> log.atLevel(level.name()).log("{} {}", message, fields))
    .build();
```

Fields are `method`, `path`, `status`, `durationMs`, `requestId`, `attempt`, `code` and, when a
retry is scheduled, `delayMs`. The API key, the publishable key, webhook secrets (yours and the
ones returned by `webhookEndpoints().create` / `rotateSecret`), query strings (including the
quoted amount), order references in a by-reference lookup and consumer data (names, phones,
emails, national ids, addresses) never reach the log at any level. `client.maskedKey()` gives
`sk_live_…AbCd` and `client.maskedPublishableKey()` `pk_live_…AbCd` for your own diagnostics.

## Security notes

- TLS only: certificate validation is the JDK's default and cannot be switched off by an SDK
  option. There is no certificate pinning.
- No redirect is ever followed; a 3xx is an `ApiException`.
- Proxies: the default `HttpClient` honours the JDK's standard proxy settings
  (`https.proxyHost` / `https.proxyPort`, or `ProxySelector.getDefault()`); there is no SDK proxy
  option.
- The client never holds anything you did not give it and never prints the key: `toString()`,
  exception messages and logs carry `maskedKey()` at most.
- Keep the API key and the webhook secret in a secrets manager; sandbox keys are `sk_test_`,
  production keys `sk_live_`, and a key is shown only once when created in the Merchant Portal.
- The publishable key (`pk_test_` / `pk_live_`) opens only the plan quote and may sit in a web
  page; the SDK still sends it only on `getPlans`, never logs it and masks it in `toString()`.
- Webhooks: verify first, acknowledge with a 2xx, process afterwards, deduplicate on the event id,
  and confirm the order with `orders().get(id)` before fulfilling.
- A webhook endpoint's secret is readable once, from the `create` or `rotateSecret` result; the
  SDK never logs it and its models' `toString()` omit it. Rotate it from the SDK or the Merchant
  Portal if it is ever exposed.

## Demo store backend

[`examples/demo-merchant-server/`](examples/demo-merchant-server/) is a complete merchant backend
for the web demo stores in plain Java (the JDK's `HttpServer`, no framework): it prices the basket
on the server, creates the checkout session, confirms the outcome with `orders().get` and verifies
webhooks on the raw body. It runs against the sandbox with your test keys, or offline against a
mock Qasimha, and serves the same HTTP API as the demo backends of the other SDKs.

```sh
cd examples/demo-merchant-server
./mvnw -q -f ../../pom.xml install -DskipTests && ./mvnw -q package
java -jar target/demo-merchant-server.jar     # see its README for mock mode and the sandbox
```

## Building and testing

Maven and JDK 11+ (`mvn verify`), or with Docker and no local toolchain:

```sh
docker run --rm -v "$PWD":/work -v "$HOME/.m2":/root/.m2 -w /work \
  maven:3.9-eclipse-temurin-17 mvn -Dmaven.compiler.release=11 verify
```

`verify` compiles with `-Werror`, runs the unit tests against an in-process HTTP server (no
network), and checks formatting with google-java-format (`mvn spotless:apply` to format).

The live smoke test runs only when both `QASIMHA_API_KEY` and `QASIMHA_BASE_URL` are set:

```sh
QASIMHA_API_KEY=sk_test_… QASIMHA_PUBLISHABLE_KEY=pk_test_… \
  QASIMHA_BASE_URL=https://api-uat.qasimha.com \
  mvn -Dtest=LiveSmokeTest test
```

It creates a three-line basket, reads it back by id and by reference, lists and counts orders,
checks the basket-identity and duplicate-reference rules, and stores an Arabic product name. With
`QASIMHA_WEBHOOKS_API_KEY` (a key holding `webhooks:manage`) it also runs a webhook endpoint round
trip — create, ping, read the delivery log, pause, rotate the secret, delete — against
`https://example.com/qasimha-sdk-test`. With `QASIMHA_PUBLISHABLE_KEY` it quotes the plans for
1200.00, checks that 50.00 is below the minimum and answers no
plans, and that an unknown publishable key is a 401.

## Support

Qasimha Integration Support — customercare@qasimha.com. Include the `requestId` from the result or
exception when reporting an API problem.

## License

MIT — see [LICENSE](LICENSE). Changes are tracked in [CHANGELOG.md](CHANGELOG.md).
