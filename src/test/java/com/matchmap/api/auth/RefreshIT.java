package com.matchmap.api.auth;

import static com.matchmap.api.support.GraphqlRequests.registerBody;
import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
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
class RefreshIT {

  private static final String PASSWORD = "correcthorse1";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void rotatesTokens() throws Exception {
    String suffix = uniqueSuffix();
    String email = "rita" + suffix + "@example.com";
    registerAccount(email, "rita_" + suffix);
    var first = refreshTokenOf(login(email));

    var response = post("/api/auth/refresh", Map.of("refreshToken", first));

    assertThat(response).hasStatus(HttpStatus.OK);
    assertThat(response).bodyJson().extractingPath("$.accessToken").asString().isNotBlank();
    assertThat(refreshTokenOf(response)).isNotBlank().isNotEqualTo(first);
  }

  @Test
  void rejectsExpiredToken() throws Exception {
    String suffix = uniqueSuffix();
    String username = "omar_" + suffix;
    registerAccount("omar" + suffix + "@example.com", username);
    UUID accountId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM user_account WHERE username = ?", UUID.class, username);
    String expired = "expired-" + suffix;
    jdbcTemplate.update(
        "INSERT INTO refresh_token (id, account_id, session_id, token_hash, created_at, expires_at)"
            + " VALUES (?, ?, ?, ?, now() - interval '31 days', now() - interval '1 day')",
        UUID.randomUUID(),
        accountId,
        UUID.randomUUID(),
        sha256(expired));

    var response = post("/api/auth/refresh", Map.of("refreshToken", expired));

    assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(response).bodyJson().extractingPath("$.error").isEqualTo("invalid_refresh_token");
  }

  @Test
  void reuseRevokesSession() throws Exception {
    String suffix = uniqueSuffix();
    String email = "sam" + suffix + "@example.com";
    registerAccount(email, "sam_" + suffix);
    var first = refreshTokenOf(login(email));
    var second = refreshTokenOf(post("/api/auth/refresh", Map.of("refreshToken", first)));

    var replay = post("/api/auth/refresh", Map.of("refreshToken", first));
    var later = post("/api/auth/refresh", Map.of("refreshToken", second));

    assertThat(replay).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(replay).bodyJson().extractingPath("$.error").isEqualTo("invalid_refresh_token");
    assertThat(later).hasStatus(HttpStatus.UNAUTHORIZED);
  }

  private static String sha256(String token) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
  }

  private void registerAccount(String email, String username) {
    mockMvc
        .post()
        .uri("/graphql")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(registerBody(email, username, "Rita", PASSWORD))
        .exchange();
  }

  private MvcTestResult login(String email) {
    return post("/api/auth/login", Map.of("email", email, "password", PASSWORD));
  }

  private String refreshTokenOf(MvcTestResult response) throws Exception {
    return JSON.readTree(response.getResponse().getContentAsString())
        .get("refreshToken")
        .asString();
  }

  private MvcTestResult post(String uri, Map<String, String> body) {
    return mockMvc
        .post()
        .uri(uri)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(JSON.writeValueAsString(body))
        .exchange();
  }
}
