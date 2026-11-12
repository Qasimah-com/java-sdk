package com.qasimha.sdk.exception;

/** The client is misconfigured, or an argument is invalid before any request is sent. */
public class ConfigurationException extends QasimhaException {

  private static final long serialVersionUID = 1L;

  public ConfigurationException(String message) {
    super(message);
  }
}
