package com.matchmap.api.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AuthPropertiesTest {

  @Test
  void refusesMissingOrShortJwtSecret() {
    var shortSecret = Base64.getEncoder().encodeToString(new byte[16]);
    var validSecret = Base64.getEncoder().encodeToString(new byte[32]);

    assertThatThrownBy(() -> properties(null)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> properties(shortSecret)).isInstanceOf(IllegalArgumentException.class);
    assertThatCode(() -> properties(validSecret)).doesNotThrowAnyException();
  }

  private static AuthProperties properties(String jwtSecret) {
    return new AuthProperties(
        jwtSecret, Duration.ofMinutes(15), Duration.ofDays(30), 5, Duration.ofMinutes(15));
  }
}
