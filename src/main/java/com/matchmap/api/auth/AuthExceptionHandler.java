package com.matchmap.api.auth;

import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthController.class)
class AuthExceptionHandler {

  @ExceptionHandler(AuthException.class)
  ResponseEntity<Map<String, String>> handle(AuthException exception) {
    var response = ResponseEntity.status(exception.status());
    if (exception.retryAfter() != null) {
      response.header(
          HttpHeaders.RETRY_AFTER,
          String.valueOf(Math.max(1L, exception.retryAfter().toSeconds())));
    }
    return response.body(Map.of("error", exception.code()));
  }
}
