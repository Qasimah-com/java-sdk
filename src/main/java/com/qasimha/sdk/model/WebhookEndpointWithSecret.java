package com.qasimha.sdk.model;

/**
 * The result of {@code webhookEndpoints().create(...)} and {@code rotateSecret(id)}: the endpoint
 * plus its signing secret, which the API returns only in this response. Store {@link #getSecret()}
 * in a secrets manager and pass it to {@code QasimhaClient.builder().webhookSecret(...)}; it is
 * never logged and {@link #toString()} does not include it.
 */
public final class WebhookEndpointWithSecret extends WebhookEndpoint {

  private String secret;

  private WebhookEndpointWithSecret() {}

  /** The signing secret ({@code whsec_...}), readable only from this response. */
  public String getSecret() {
    return secret;
  }
}
