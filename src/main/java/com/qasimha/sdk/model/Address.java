package com.qasimha.sdk.model;

/** A shipping or billing address. */
public final class Address {

  private String firstName;
  private String lastName;
  private String line1;
  private String line2;
  private String city;
  private String region;
  private String postalCode;
  private String countryCode;
  private String phoneNumber;

  private Address() {}

  private Address(Builder builder) {
    this.firstName = builder.firstName;
    this.lastName = builder.lastName;
    this.line1 = builder.line1;
    this.line2 = builder.line2;
    this.city = builder.city;
    this.region = builder.region;
    this.postalCode = builder.postalCode;
    this.countryCode = builder.countryCode;
    this.phoneNumber = builder.phoneNumber;
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

  public String getLine1() {
    return line1;
  }

  public String getLine2() {
    return line2;
  }

  public String getCity() {
    return city;
  }

  public String getRegion() {
    return region;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  /** Builds an {@link Address}. */
  public static final class Builder {
    private String firstName;
    private String lastName;
    private String line1;
    private String line2;
    private String city;
    private String region;
    private String postalCode;
    private String countryCode = "SA";
    private String phoneNumber;

    private Builder() {}

    public Builder firstName(String firstName) {
      this.firstName = firstName;
      return this;
    }

    public Builder lastName(String lastName) {
      this.lastName = lastName;
      return this;
    }

    public Builder line1(String line1) {
      this.line1 = line1;
      return this;
    }

    public Builder line2(String line2) {
      this.line2 = line2;
      return this;
    }

    public Builder city(String city) {
      this.city = city;
      return this;
    }

    public Builder region(String region) {
      this.region = region;
      return this;
    }

    public Builder postalCode(String postalCode) {
      this.postalCode = postalCode;
      return this;
    }

    public Builder countryCode(String countryCode) {
      this.countryCode = countryCode;
      return this;
    }

    public Builder phoneNumber(String phoneNumber) {
      this.phoneNumber = phoneNumber;
      return this;
    }

    public Address build() {
      return new Address(this);
    }
  }
}
