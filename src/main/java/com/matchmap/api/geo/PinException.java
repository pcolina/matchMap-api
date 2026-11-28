package com.matchmap.api.geo;

class PinException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String code;
  private final String field;

  PinException(String code, String field, String message) {
    super(message, null, false, false);
    this.code = code;
    this.field = field;
  }

  static PinException cityNotFound() {
    return new PinException("VALIDATION_FAILED", "cityId", "City does not exist");
  }

  static PinException cityRetired() {
    return new PinException("VALIDATION_FAILED", "cityId", "City is retired");
  }

  static PinException invalidCityId() {
    return new PinException("VALIDATION_FAILED", "cityId", "Invalid city id");
  }

  String code() {
    return code;
  }

  String field() {
    return field;
  }
}
