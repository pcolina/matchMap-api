package com.matchmap.api.account;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderTest {

  @Test
  void defaultEncoderIsArgon2id() {
    PasswordEncoder encoder = new PasswordEncoderConfiguration().passwordEncoder();

    assertThat(encoder.encode("correct horse battery")).startsWith("{argon2@SpringSecurity_v5_8}");
  }
}
