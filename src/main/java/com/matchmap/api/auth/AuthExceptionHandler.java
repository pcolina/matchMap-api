package com.matchmap.api.auth;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthController.class)
class AuthExceptionHandler {

  @ExceptionHandler(AuthException.class)
  ResponseEntity<Map<String, String>> handle(AuthException exception) {
    return ResponseEntity.status(exception.status()).body(Map.of("error", exception.code()));
  }
}
