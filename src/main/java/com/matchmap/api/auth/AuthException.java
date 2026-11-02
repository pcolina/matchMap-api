package com.matchmap.api.auth;

import org.springframework.http.HttpStatus;

class AuthException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final HttpStatus status;
  private final String code;

  AuthException(HttpStatus status, String code) {
    super(code, null, false, false);
    this.status = status;
    this.code = code;
  }

  static AuthException invalidCredentials() {
    return new AuthException(HttpStatus.UNAUTHORIZED, "invalid_credentials");
  }

  HttpStatus status() {
    return status;
  }

  String code() {
    return code;
  }
}
