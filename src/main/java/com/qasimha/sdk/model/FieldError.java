package com.qasimha.sdk.model;

import java.util.Objects;

/**
 * One field-level error, either reported by the API in its own PascalCase field naming ({@code
 * TotalAmount}, {@code Items[0].Quantity}) or raised client-side before a request is sent.
 */
public final class FieldError {

  private String field;
  private String message;

  private FieldError() {}

  public FieldError(String field, String message) {
    this.field = field;
    this.message = message;
  }

  /** The field the error refers to, as named by the API. */
  public String getField() {
    return field;
  }

  /** The human-readable message. */
  public String getMessage() {
    return message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FieldError)) {
      return false;
    }
    FieldError that = (FieldError) o;
    return Objects.equals(field, that.field) && Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(field, message);
  }

  @Override
  public String toString() {
    return field + ": " + message;
  }
}
