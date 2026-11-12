package com.qasimha.sdk.model;

/** The shopper a session is created for. */
public final class Consumer {

  private String firstName;
  private String lastName;
  private String phoneNumber;
  private String email;
  private String nationalId;

  private Consumer() {}

  private Consumer(Builder builder) {
    this.firstName = builder.firstName;
    this.lastName = builder.lastName;
    this.phoneNumber = builder.phoneNumber;
    this.email = builder.email;
    this.nationalId = builder.nationalId;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public String getEmail() {
    return email;
  }

  /** A Saudi national id or iqama: ten digits starting with 1 or 2. */
  public String getNationalId() {
    return nationalId;
  }

  /** Builds a {@link Consumer}. */
  public static final class Builder {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String nationalId;

    private Builder() {}

    public Builder firstName(String firstName) {
      this.firstName = firstName;
      return this;
    }

    public Builder lastName(String lastName) {
      this.lastName = lastName;
      return this;
    }

    public Builder phoneNumber(String phoneNumber) {
      this.phoneNumber = phoneNumber;
      return this;
    }

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder nationalId(String nationalId) {
      this.nationalId = nationalId;
      return this;
    }

    public Consumer build() {
      return new Consumer(this);
    }
  }
}
