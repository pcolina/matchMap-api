package com.matchmap.api.auth;

import static com.matchmap.api.support.GraphqlRequests.registerBody;
import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
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

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class LoginIT {

  private static final String PASSWORD = "correct horse battery";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

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

    var response = login("cara" + suffix + "@example.com", "wrong password here");

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
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(JSON.writeValueAsString(Map.of("email", email, "password", password)))
        .exchange();
  }
}
