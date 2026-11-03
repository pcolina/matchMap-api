package com.matchmap.api.auth;

import java.time.Duration;
import org.springframework.http.HttpStatus;

class AuthException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final HttpStatus status;
  private final String code;
  private final Duration retryAfter;

  private AuthException(HttpStatus status, String code, Duration retryAfter) {
    super(code, null, false, false);
    this.status = status;
    this.code = code;
    this.retryAfter = retryAfter;
  }

  static AuthException invalidCredentials() {
    return new AuthException(HttpStatus.UNAUTHORIZED, "invalid_credentials", null);
  }

  static AuthException invalidRefreshToken() {
    return new AuthException(HttpStatus.UNAUTHORIZED, "invalid_refresh_token", null);
  }

  static AuthException invalidGoogleToken() {
    return new AuthException(HttpStatus.UNAUTHORIZED, "invalid_google_token", null);
  }

  static AuthException googleUnavailable() {
    return new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "google_unavailable", null);
  }

  static AuthException googleEmailNotVerified() {
    return new AuthException(HttpStatus.FORBIDDEN, "google_email_not_verified", null);
  }

  static AuthException tooManyAttempts(Duration retryAfter) {
    return new AuthException(HttpStatus.TOO_MANY_REQUESTS, "too_many_attempts", retryAfter);
  }

  HttpStatus status() {
    return status;
  }

  String code() {
    return code;
  }

  Duration retryAfter() {
    return retryAfter;
  }
}
