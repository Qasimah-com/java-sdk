package com.qasimha.sdk.exception;

import java.util.Map;

/** A 401: the API key is missing, invalid or revoked. The body is usually empty. */
public class AuthenticationException extends QasimhaException {

  private static final long serialVersionUID = 1L;

  public AuthenticationException(
      String message, String code, String requestId, Map<String, Object> rawEnvelope) {
    super(message, code, 401, requestId, null, rawEnvelope, null);
  }
}
