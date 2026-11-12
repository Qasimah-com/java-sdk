package com.qasimha.sdk.internal;

import com.qasimha.sdk.exception.ValidationException;
import com.qasimha.sdk.model.CreateWebhookEndpointRequest;
import com.qasimha.sdk.model.FieldError;
import com.qasimha.sdk.model.UpdateWebhookEndpointRequest;
import com.qasimha.sdk.model.WebhookEventTypes;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Client-side checks for webhook endpoint requests: the URL must be {@code https://} and every
 * event must be a known type. The API remains the authority and additionally refuses private
 * network targets.
 */
public final class WebhookEndpointValidator {

  private static final int MAX_URL_LENGTH = 1024;

  private WebhookEndpointValidator() {}

  public static void validate(CreateWebhookEndpointRequest request) {
    List<FieldError> errors = new ArrayList<>();
    url(errors, request.getUrl(), true);
    events(errors, request.getEvents());
    raise(errors);
  }

  public static void validate(UpdateWebhookEndpointRequest request) {
    List<FieldError> errors = new ArrayList<>();
    if (request.getUrl() != null) {
      url(errors, request.getUrl(), true);
    }
    events(errors, request.getEvents());
    raise(errors);
  }

  private static void url(List<FieldError> errors, String value, boolean required) {
    if (value == null || value.trim().isEmpty()) {
      if (required) {
        errors.add(new FieldError("Url", "is required"));
      }
      return;
    }
    String trimmed = value.trim();
    if (trimmed.length() > MAX_URL_LENGTH) {
      errors.add(new FieldError("Url", "must be " + MAX_URL_LENGTH + " characters or fewer"));
      return;
    }
    URI uri;
    try {
      uri = new URI(trimmed);
    } catch (URISyntaxException e) {
      errors.add(new FieldError("Url", "is not a valid absolute URL"));
      return;
    }
    String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
    if (!scheme.equals("https")) {
      errors.add(new FieldError("Url", "must use https://"));
      return;
    }
    if (uri.getHost() == null || uri.getHost().isEmpty()) {
      errors.add(new FieldError("Url", "must include a host"));
    }
  }

  private static void events(List<FieldError> errors, List<String> events) {
    if (events == null) {
      return;
    }
    for (int i = 0; i < events.size(); i++) {
      String event = events.get(i);
      if (!WebhookEventTypes.isKnown(event)) {
        errors.add(
            new FieldError(
                "Events[" + i + "]",
                "'" + event + "' is not a known event type; see WebhookEventTypes"));
      }
    }
  }

  private static void raise(List<FieldError> errors) {
    if (!errors.isEmpty()) {
      throw ValidationException.clientSide(errors);
    }
  }
}
