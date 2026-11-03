package com.matchmap.api.auth;

import java.time.Duration;
import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("matchmap.auth")
record AuthProperties(
    String jwtSecret,
    Duration accessTokenTtl,
    Duration refreshTokenTtl,
    int maxFailedLogins,
    Duration failedLoginWindow) {

  AuthProperties {
    if (jwtSecret == null || Base64.getDecoder().decode(jwtSecret).length < 32) {
      throw new IllegalArgumentException(
          "matchmap.auth.jwt-secret must be a base64 value of at least 256 bits");
    }
  }
}
