package com.matchmap.api.account;

class AccountException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String code;
  private final String field;

  AccountException(String code, String field, String message) {
    super(message, null, false, false);
    this.code = code;
    this.field = field;
  }

  static AccountException emailTaken() {
    return new AccountException("EMAIL_TAKEN", null, "Email is already in use");
  }

  static AccountException usernameTaken() {
    return new AccountException("USERNAME_TAKEN", null, "Username is already in use");
  }

  static AccountException validationFailed(String field) {
    return new AccountException("VALIDATION_FAILED", field, "Invalid " + field);
  }

  String code() {
    return code;
  }

  String field() {
    return field;
  }
}
