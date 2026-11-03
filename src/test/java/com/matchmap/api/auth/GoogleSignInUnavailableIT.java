package com.matchmap.api.auth;

import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

@Import({TestcontainersConfiguration.class, GoogleSignInUnavailableIT.UnavailableJwks.class})
@SpringBootTest
@AutoConfigureMockMvc
class GoogleSignInUnavailableIT {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvcTester mockMvc;

  @Test
  void answersServiceUnavailableWhenGoogleKeysCannotBeFetched() throws Exception {
    String suffix = uniqueSuffix();
    var now = Instant.now();
    var claims =
        new JWTClaimsSet.Builder()
            .issuer("https://accounts.google.com")
            .subject("g-" + suffix)
            .audience("matchmap-test-client")
            .claim("email", "pia" + suffix + "@gmail.com")
            .claim("email_verified", true)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(3600)))
            .build();
    var jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(GoogleJwksTestConfiguration.KEY.getKeyID())
                .build(),
            claims);
    jwt.sign(new RSASSASigner(GoogleJwksTestConfiguration.KEY));

    var response = signIn(jwt.serialize());

    assertThat(response).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response).bodyJson().extractingPath("$.error").isEqualTo("google_unavailable");
  }

  private MvcTestResult signIn(String idToken) {
    return mockMvc
        .post()
        .uri("/api/auth/google")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(JSON.writeValueAsString(Map.of("idToken", idToken)))
        .exchange();
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class UnavailableJwks {

    @Bean
    @Primary
    JWKSource<SecurityContext> unavailableGoogleJwks() {
      return (selector, context) -> {
        throw new KeySourceException("Google JWK set unavailable (simulated)");
      };
    }
  }
}
