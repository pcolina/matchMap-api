package com.matchmap.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(GoogleJwkRetryTest.Config.class)
class GoogleJwkRetryTest {

  @Autowired FlakyJwkSource flaky;

  @Autowired
  @Qualifier("googleJwkSource")
  JWKSource<SecurityContext> jwks;

  @BeforeEach
  void resetCalls() {
    flaky.reset();
  }

  @Test
  void retriesFetchUntilKeysAreAvailable() throws Exception {
    flaky.failTimes(2);

    var keys = jwks.get(null, null);

    assertThat(keys).isEmpty();
    assertThat(flaky.calls()).isEqualTo(3);
  }

  @Test
  void givesUpAfterThreeAttempts() {
    flaky.failTimes(10);

    assertThatThrownBy(() -> jwks.get(null, null)).isInstanceOf(KeySourceException.class);
    assertThat(flaky.calls()).isEqualTo(3);
  }

  @Configuration(proxyBeanMethods = false)
  @EnableResilientMethods(proxyTargetClass = true)
  static class Config {

    @Bean
    FlakyJwkSource flakyJwkSource() {
      return new FlakyJwkSource();
    }

    @Bean
    JWKSource<SecurityContext> googleJwkSource(FlakyJwkSource flaky) {
      return new ResilientJwkSource(flaky);
    }
  }

  static final class FlakyJwkSource implements JWKSource<SecurityContext> {

    private int failures;
    private int calls;

    void reset() {
      failures = 0;
      calls = 0;
    }

    void failTimes(int count) {
      failures = count;
    }

    int calls() {
      return calls;
    }

    @Override
    public List<JWK> get(JWKSelector selector, SecurityContext context) throws KeySourceException {
      calls++;
      if (calls <= failures) {
        throw new KeySourceException("Google JWK set unavailable (simulated)");
      }
      return List.of();
    }
  }
}
