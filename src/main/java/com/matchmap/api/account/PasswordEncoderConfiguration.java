package com.matchmap.api.account;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
class PasswordEncoderConfiguration {

  private static final String ARGON2_ID = "argon2@SpringSecurity_v5_8";

  @Bean
  PasswordEncoder passwordEncoder() {
    return new DelegatingPasswordEncoder(
        ARGON2_ID, Map.of(ARGON2_ID, Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
  }
}
