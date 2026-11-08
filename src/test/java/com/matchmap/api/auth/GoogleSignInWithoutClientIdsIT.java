package com.matchmap.api.auth;

import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

@Import({TestcontainersConfiguration.class, GoogleJwksTestConfiguration.class})
@SpringBootTest(properties = "matchmap.auth.google.client-ids=")
@AutoConfigureMockMvc
class GoogleSignInWithoutClientIdsIT {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvcTester mockMvc;

  @Test
  void rejectsEveryGoogleSignInWhenNoClientIdIsConfigured() throws Exception {
    String suffix = uniqueSuffix();
    var now = Instant.now();
    var claims =
        new JWTClaimsSet.Builder()
            .issuer("https://accounts.google.com")
            .subject("g-" + suffix)
            .audience("matchmap-test-client")
            .claim("email", "nina" + suffix + "@gmail.com")
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

    var response =
        mockMvc
            .post()
            .uri("/api/auth/google")
            .header("X-Client", "mobile")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(JSON.writeValueAsString(Map.of("idToken", jwt.serialize())))
            .exchange();

    assertInvalidGoogleToken(response);
  }

  private void assertInvalidGoogleToken(MvcTestResult response) {
    assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(response).bodyJson().extractingPath("$.error").isEqualTo("invalid_google_token");
  }
}
