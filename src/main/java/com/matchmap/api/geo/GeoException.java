package com.matchmap.api.geo;

class GeoException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String code;
  private final String field;

  GeoException(String code, String field, String message) {
    super(message, null, false, false);
    this.code = code;
    this.field = field;
  }

  static GeoException cityNotFound() {
    return new GeoException("VALIDATION_FAILED", "cityId", "City does not exist");
  }

  static GeoException cityRetired() {
    return new GeoException("VALIDATION_FAILED", "cityId", "City is retired");
  }

  static GeoException invalidCityId() {
    return new GeoException("VALIDATION_FAILED", "cityId", "Invalid city id");
  }

  static GeoException invalid(String field, String message) {
    return new GeoException("VALIDATION_FAILED", field, message);
  }

  String code() {
    return code;
  }

  String field() {
    return field;
  }
}
