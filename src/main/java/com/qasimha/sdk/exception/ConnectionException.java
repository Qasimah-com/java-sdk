package com.qasimha.sdk.exception;

/** A DNS, TLS, socket or timeout failure; the transport exception is the cause. */
public class ConnectionException extends QasimhaException {

  private static final long serialVersionUID = 1L;

  public ConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
