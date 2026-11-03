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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

@Import({TestcontainersConfiguration.class, GoogleJwksTestConfiguration.class})
@SpringBootTest
@AutoConfigureMockMvc
class GoogleSignInIT {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void createsAccountOnFirstSignIn() throws Exception {
    String suffix = uniqueSuffix();
    String subject = "g-" + suffix;
    String email = "carla" + suffix + "@gmail.com";

    var response = signIn(googleToken(subject, email, "Carla"));

    assertThat(response).hasStatus(HttpStatus.OK);
    assertThat(response).bodyJson().extractingPath("$.accessToken").asString().isNotBlank();
    assertThat(response).bodyJson().extractingPath("$.refreshToken").asString().isNotBlank();
    var account =
        jdbcTemplate.queryForMap(
            "SELECT email, username, display_name, password_hash FROM user_account"
                + " WHERE google_subject = ?",
            subject);
    assertThat(account)
        .containsEntry("email", email)
        .containsEntry("display_name", "Carla")
        .containsEntry("username", "carla" + suffix);
    assertThat(account.get("password_hash")).isNull();
  }

  @Test
  void usesEmailLocalPartAsDisplayNameWhenNameIsMissing() throws Exception {
    String suffix = uniqueSuffix();
    String subject = "g-" + suffix;
    String email = "erin" + suffix + "@gmail.com";

    signIn(googleToken(subject, email, null));

    assertThat(displayNameOf(subject)).isEqualTo("erin" + suffix);
  }

  @Test
  void cutsDisplayNameToFiftyCharacters() throws Exception {
    String suffix = uniqueSuffix();
    String subject = "g-" + suffix;
    String email = "fay" + suffix + "@gmail.com";

    signIn(googleToken(subject, email, "N".repeat(60)));

    assertThat(displayNameOf(subject)).isEqualTo("N".repeat(50));
  }

  @Test
  void signsInReturningUser() throws Exception {
    String suffix = uniqueSuffix();
    String subject = "g-" + suffix;
    String email = "dana" + suffix + "@gmail.com";
    signIn(googleToken(subject, email, "Dana"));

    var response = signIn(googleToken(subject, email, "Dana"));

    assertThat(response).hasStatus(HttpStatus.OK);
    Integer accounts =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM user_account WHERE google_subject = ?", Integer.class, subject);
    UUID accountId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM user_account WHERE google_subject = ?", UUID.class, subject);
    assertThat(accounts).isEqualTo(1);
    assertThat(SignedJWT.parse(accessToken(response)).getJWTClaimsSet().getSubject())
        .isEqualTo(accountId.toString());
  }

  @Test
  void rejectsTokenForAnotherAudience() throws Exception {
    String suffix = uniqueSuffix();

    var response =
        signIn(
            googleToken(
                "https://accounts.google.com",
                "another-client",
                Instant.now().plusSeconds(3600),
                "g-" + suffix,
                "gina" + suffix + "@gmail.com",
                "Gina"));

    assertInvalidGoogleToken(response);
  }

  @Test
  void rejectsExpiredToken() throws Exception {
    String suffix = uniqueSuffix();

    var response =
        signIn(
            googleToken(
                "https://accounts.google.com",
                "matchmap-test-client",
                Instant.now().minusSeconds(600),
                "g-" + suffix,
                "hana" + suffix + "@gmail.com",
                "Hana"));

    assertInvalidGoogleToken(response);
  }

  @Test
  void rejectsTokenFromAnotherIssuer() throws Exception {
    String suffix = uniqueSuffix();

    var response =
        signIn(
            googleToken(
                "https://evil.example.com",
                "matchmap-test-client",
                Instant.now().plusSeconds(3600),
                "g-" + suffix,
                "ivy" + suffix + "@gmail.com",
                "Ivy"));

    assertInvalidGoogleToken(response);
  }

  private void assertInvalidGoogleToken(MvcTestResult response) {
    assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(response).bodyJson().extractingPath("$.error").isEqualTo("invalid_google_token");
  }

  private String displayNameOf(String subject) {
    return jdbcTemplate.queryForObject(
        "SELECT display_name FROM user_account WHERE google_subject = ?", String.class, subject);
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

  private String accessToken(MvcTestResult response) throws Exception {
    return JSON.readTree(response.getResponse().getContentAsString()).get("accessToken").asString();
  }

  private static String googleToken(String subject, String email, String name) throws Exception {
    return googleToken(
        "https://accounts.google.com",
        "matchmap-test-client",
        Instant.now().plusSeconds(3600),
        subject,
        email,
        name);
  }

  private static String googleToken(
      String issuer, String audience, Instant expiresAt, String subject, String email, String name)
      throws Exception {
    var claims =
        new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(subject)
            .audience(audience)
            .claim("email", email)
            .claim("email_verified", true)
            .claim("name", name)
            .issueTime(Date.from(expiresAt.minusSeconds(3600)))
            .expirationTime(Date.from(expiresAt))
            .build();
    var jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(GoogleJwksTestConfiguration.KEY.getKeyID())
                .build(),
            claims);
    jwt.sign(new RSASSASigner(GoogleJwksTestConfiguration.KEY));
    return jwt.serialize();
  }
}
