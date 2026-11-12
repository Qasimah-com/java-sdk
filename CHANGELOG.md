# Changelog

All notable changes to `com.qasimha:qasimha-sdk` are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `checkout().getPlans(amount)` and `getPlans(amount, GetPlansOptions)`: the instalment plans
  the merchant's policy offers for an amount (`GET /widget/plans?amount=&currency=SAR&lang=`),
  with the models `Plans` (`merchantName`, `currency`, `minAmount`, `maxAmount`, `plans`,
  `isOffered()`) and `Plan`. The request authenticates with the publishable key in
  `X-Public-Key` and sends no `X-Api-Key`; `GetPlansOptions` overrides the language and the key
  per call. An amount outside the policy's range answers an empty list. The amount must be
  positive with at most two decimals (client-side `ValidationException`); 401 is an
  `AuthenticationException`, 400 a `ValidationException`; the call is retried like every `GET`.
- `QasimhaClient.Builder.publishableKey(String)`: an optional `pk_test_…` / `pk_live_…` key whose
  environment must be the API key's (`ConfigurationException` otherwise). Without one on the
  client or the call, `getPlans` throws `ConfigurationException` before any request. The key is
  never logged; `maskedPublishableKey()` and `toString()` show `pk_live_…AbCd`.
- `Environment.getPublishableKeyPrefix()` and `Environment.forPublishableKey(String)`.
- `orders().getByReference(orderReferenceId)`: the same `Order` as `get`, looked up by the
  merchant's own reference (`GET /merchant/orders/by-reference/{orderReferenceId}`). Each path
  segment is percent-encoded and `/` is kept; the reference is not written to the SDK log.
- `webhookEndpoints()`: `create`, `list`, `get`, `update`, `delete`, `rotateSecret`, `ping`,
  `listDeliveries`, `replayDelivery` against `/merchant/webhooks` (scope `webhooks:manage`), with
  the models `WebhookEndpoint`, `WebhookEndpointWithSecret`, `WebhookDelivery`, `PingResult`,
  `CreateWebhookEndpointRequest`, `UpdateWebhookEndpointRequest`, `ListDeliveriesParams` and the
  `WebhookEventTypes` constants. The URL must be `https://` and every event a known type; both
  are checked client-side. The secret returned by `create` and `rotateSecret` is never logged and
  is omitted from `toString()`.
- `PUT` and `DELETE` requests carry an `Idempotency-Key` and retry under it like `POST`.

### Changed

- `getRaw()` on orders follows the typed order responses of the API: enum names instead of
  integers, and internal fields that are not part of the contract are no longer included.
  `StatusChange.getMetadata()` is always null because the API no longer sends it.

## [0.1.0] - 2026-09-25

### Added

- `QasimhaClient` builder with environment inference from the API key prefix, `baseUrl`
  override, per-attempt timeout, bounded retries, `Accept-Language`, `User-Agent` suffix,
  optional logger callback and injectable `java.net.http.HttpClient`.
- `checkout()`: `createSession`, `createPosSession`, `createQrSession` with client-side basket
  validation (two-decimal amounts, non-negative amounts, line and cart identities to the halala).
- `orders()`: `list`, `get`, `capture`, `cancel`, `refund`, `fulfill`, `counts`, `contract`.
- `checkoutLinks()`: `create`, `list`, `get`, `cancel`.
- `settlements()`: `list`, `overview`.
- `webhooks()`: `constructEvent` and `verifySignature` for both the `sha256=` and the
  `t=…,v1=…` signature formats, constant-time comparison, replay tolerance.
- Enum normalisation: integer or string `status`, `channel`, `paymentType` and instalment
  `status` values are exposed by name; unknown values pass through unchanged.
- Error model: `QasimhaException` with `ConfigurationException`, `ValidationException`,
  `AuthenticationException`, `PermissionException`, `NotFoundException`,
  `DuplicateOrderReferenceException`, `ConflictException`, `OrderDeclinedException`,
  `RateLimitException`, `ApiException`, `ServerException`, `ConnectionException`,
  `WebhookSignatureException`.
- Retries on connection failures, 429, 502, 503, 504 and `409 REQUEST_IN_PROGRESS`, with
  exponential backoff, jitter and `Retry-After` support; a UUID v4 `Idempotency-Key` and
  `X-Correlation-Id` per logical call.
