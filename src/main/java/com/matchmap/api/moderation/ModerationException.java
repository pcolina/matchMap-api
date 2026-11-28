package com.matchmap.api.moderation;

class ModerationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String code;
  private final String field;

  ModerationException(String code, String field, String message) {
    super(message, null, false, false);
    this.code = code;
    this.field = field;
  }

  static ModerationException duplicateWord() {
    return new ModerationException("DUPLICATE_WORD", null, "Word already exists in this category");
  }

  static ModerationException invalid(String field, String message) {
    return new ModerationException("VALIDATION_FAILED", field, message);
  }

  String code() {
    return code;
  }

  String field() {
    return field;
  }
}
