package com.qasimha.sdk.exception;

/** A webhook delivery failed verification; {@link #getReason()} says why. */
public class WebhookSignatureException extends QasimhaException {

  private static final long serialVersionUID = 1L;

  /** Why the delivery was rejected. */
  public enum Reason {
    MISSING_HEADER("missing_header"),
    MALFORMED_HEADER("malformed_header"),
    TIMESTAMP_OUTSIDE_TOLERANCE("timestamp_outside_tolerance"),
    SIGNATURE_MISMATCH("signature_mismatch");

    private final String code;

    Reason(String code) {
      this.code = code;
    }

    /** The snake_case reason shared by every Qasimha SDK. */
    public String getCode() {
      return code;
    }
  }

  private final Reason reason;

  public WebhookSignatureException(Reason reason, String message) {
    super(message, reason.getCode(), null, null, null, null, null);
    this.reason = reason;
  }

  /** The rejection reason. */
  public Reason getReason() {
    return reason;
  }
}
