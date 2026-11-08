package com.matchmap.api.auth;

import static com.matchmap.api.support.GraphqlRequests.registerBody;
import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.matchmap.api.TestcontainersConfiguration;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class LoginIT {

  private static final String PASSWORD = "Correcthorse1!";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

  @Autowired PasswordEncoder passwordEncoder;

  @Test
  void returnsTokensForCorrectCredentials() {
    String suffix = uniqueSuffix();
    registerAccount("ana" + suffix + "@example.com", "ana_" + suffix);

    var response = login(" Ana" + suffix + "@Example.com ", PASSWORD);

    assertThat(response).hasStatus(HttpStatus.OK);
    assertThat(response).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
    assertThat(response).bodyJson().extractingPath("$.accessToken").asString().isNotBlank();
    assertThat(response).bodyJson().extractingPath("$.expiresIn").isEqualTo(900);
    assertThat(response).bodyJson().extractingPath("$.refreshToken").asString().isNotBlank();
  }

  @Test
  void expiresInMatchesAccessTokenLifetime() throws Exception {
    String suffix = uniqueSuffix();
    registerAccount("bob" + suffix + "@example.com", "bob_" + suffix);

    var response = login("bob" + suffix + "@example.com", PASSWORD);

    var claims = SignedJWT.parse(accessToken(response)).getJWTClaimsSet();
    var lifetime =
        Duration.between(claims.getIssueTime().toInstant(), claims.getExpirationTime().toInstant());
    assertThat(response).bodyJson().extractingPath("$.expiresIn").isEqualTo(900);
    assertThat(lifetime).isEqualTo(Duration.ofSeconds(900));
  }

  @Test
  void rejectsWrongPassword() {
    String suffix = uniqueSuffix();
    registerAccount("cara" + suffix + "@example.com", "cara_" + suffix);

    var response = login("cara" + suffix + "@example.com", "wrongpassword1");

    assertInvalidCredentials(response);
  }

  @Test
  void rejectsUnknownEmail() {
    var response = login("nobody" + uniqueSuffix() + "@example.com", PASSWORD);

    assertInvalidCredentials(response);
  }

  @Test
  void rejectsAccountWithoutPassword() {
    String suffix = uniqueSuffix();
    jdbcTemplate.update(
        "INSERT INTO user_account (id, email, username, display_name, created_at)"
            + " VALUES (?, ?, ?, ?, now())",
        UUID.randomUUID(),
        "gina" + suffix + "@example.com",
        "gina_" + suffix,
        "Gina");

    var response = login("gina" + suffix + "@example.com", PASSWORD);

    assertInvalidCredentials(response);
  }

  @Test
  void rejectsEmptyEmail() {
    var response = login("", PASSWORD);

    assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(response).bodyJson().extractingPath("$.error").isEqualTo("invalid_email");
  }

  @Test
  void rejectsEmailTooLong() {
    var response = login("a".repeat(243) + "@example.com", PASSWORD);

    assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(response).bodyJson().extractingPath("$.error").isEqualTo("invalid_email");
  }

  @Test
  void rejectsEmailNotInStandardFormat() {
    var response = login("not-an-email", PASSWORD);

    assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(response).bodyJson().extractingPath("$.error").isEqualTo("invalid_email");
  }

  @Test
  void rejectsPasswordTooShort() {
    var response = login("short" + uniqueSuffix() + "@example.com", "abcde");

    assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(response).bodyJson().extractingPath("$.error").isEqualTo("invalid_password");
  }

  @Test
  void rejectsPasswordTooLong() {
    var response = login("long" + uniqueSuffix() + "@example.com", "a".repeat(21));

    assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(response).bodyJson().extractingPath("$.error").isEqualTo("invalid_password");
  }

  @Test
  void passwordWithoutUppercaseIsNotRejectedForThat() {
    String suffix = uniqueSuffix();
    String password = "lowercase123";
    jdbcTemplate.update(
        "INSERT INTO user_account (id, email, username, display_name, password_hash, created_at)"
            + " VALUES (?, ?, ?, ?, ?, now())",
        UUID.randomUUID(),
        "lower" + suffix + "@example.com",
        "lower_" + suffix,
        "Lo",
        passwordEncoder.encode(password));

    var response = login("lower" + suffix + "@example.com", password);

    assertThat(response).hasStatus(HttpStatus.OK);
    assertThat(response).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
  }

  @Test
  void limitBreachIsNotLoggedWithSubmittedValues() {
    var email = "breach" + uniqueSuffix() + "@example.com";
    var password = "secr3";
    var logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    var appender = new ListAppender<ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    try {
      var response = login(email, password);

      assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
      assertThat(appender.list)
          .allSatisfy(
              event -> {
                assertThat(event.getFormattedMessage())
                    .doesNotContain(email)
                    .doesNotContain(password);
                assertThat(String.valueOf(event.getThrowableProxy()))
                    .doesNotContain(email)
                    .doesNotContain(password);
              });
    } finally {
      logger.detachAppender(appender);
    }
  }

  @Test
  void rejectsWhitespaceInsidePassword() {
    var response = login("space" + uniqueSuffix() + "@example.com", "secret pass1");

    assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(response).bodyJson().extractingPath("$.error").isEqualTo("invalid_password");
  }

  @Test
  void trimsSurroundingSpacesOfEmailAndPassword() {
    String suffix = uniqueSuffix();
    registerAccount("surround" + suffix + "@example.com", "surround_" + suffix);

    var response = login("  surround" + suffix + "@example.com  ", " " + PASSWORD + " ");

    assertThat(response).hasStatus(HttpStatus.OK);
    assertThat(response).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
  }

  @Test
  void matchesEmailRegardlessOfLetterCase() {
    String suffix = uniqueSuffix();
    registerAccount("lettercase" + suffix + "@example.com", "lettercase_" + suffix);

    var response = login("LetterCase" + suffix + "@EXAMPLE.com", PASSWORD);

    assertThat(response).hasStatus(HttpStatus.OK);
    assertThat(response).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
  }

  private void assertInvalidCredentials(MvcTestResult response) {
    assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(response).bodyJson().extractingPath("$.error").isEqualTo("invalid_credentials");
  }

  private String accessToken(MvcTestResult response) throws Exception {
    return JSON.readTree(response.getResponse().getContentAsString()).get("accessToken").asString();
  }

  private void registerAccount(String email, String username) {
    mockMvc
        .post()
        .uri("/graphql")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(registerBody(email, username, "Ana", PASSWORD))
        .exchange();
  }

  private MvcTestResult login(String email, String password) {
    return mockMvc
        .post()
        .uri("/api/auth/login")
        .header("X-Client", "mobile")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(JSON.writeValueAsString(Map.of("email", email, "password", password)))
        .exchange();
  }
}
