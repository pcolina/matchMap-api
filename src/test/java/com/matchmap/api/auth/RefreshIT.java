package com.matchmap.api.auth;

import static com.matchmap.api.support.GraphqlRequests.registerBody;
import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
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

  private static final String PASSWORD = "Correcthorse1!";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void rotatesTokens() throws Exception {
    String suffix = uniqueSuffix();
    String email = "rita" + suffix + "@example.com";
    registerAccount(email);
    var first = refreshTokenOf(login(email));

    var response = post("/api/auth/refresh", Map.of("refreshToken", first));

    assertThat(response).hasStatus(HttpStatus.OK);
    assertThat(response).bodyJson().extractingPath("$.accessToken").asString().isNotBlank();
    assertThat(refreshTokenOf(response)).isNotBlank().isNotEqualTo(first);
  }

  @Test
  void rejectsExpiredToken() throws Exception {
    String suffix = uniqueSuffix();
    String username = "omar" + suffix;
    registerAccount("omar" + suffix + "@example.com");
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
    registerAccount(email);
    var first = refreshTokenOf(login(email));
    var second = refreshTokenOf(post("/api/auth/refresh", Map.of("refreshToken", first)));

    var replay = post("/api/auth/refresh", Map.of("refreshToken", first));
    var later = post("/api/auth/refresh", Map.of("refreshToken", second));

    assertThat(replay).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(replay).bodyJson().extractingPath("$.error").isEqualTo("invalid_refresh_token");
    assertThat(later).hasStatus(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void successfulRefreshFromMobileReturnsRefreshTokenInBody() throws Exception {
    String suffix = uniqueSuffix();
    String email = "tina" + suffix + "@example.com";
    registerAccount(email);
    var first = refreshTokenOf(login(email));

    var response = post("/api/auth/refresh", Map.of("refreshToken", first));

    assertThat(response).hasStatus(HttpStatus.OK);
    assertThat(response).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
    assertThat(response).bodyJson().extractingPath("$.accessToken").asString().isNotBlank();
    assertThat(response).bodyJson().extractingPath("$.expiresIn").isEqualTo(900);
    assertThat(refreshTokenOf(response)).isNotBlank().isNotEqualTo(first);
  }

  @Test
  void successfulRefreshFromWebUsesCookieAndReturnsNewCookie() throws Exception {
    String suffix = uniqueSuffix();
    String email = "ursula" + suffix + "@example.com";
    registerAccount(email);
    var first = refreshCookieOf(webLogin(email));

    var response = webRefresh(first);

    assertThat(response).hasStatus(HttpStatus.OK);
    assertThat(response).bodyJson().extractingPath("$.accessToken").asString().isNotBlank();
    assertThat(JSON.readTree(response.getResponse().getContentAsString()).has("refreshToken"))
        .isFalse();
    assertThat(refreshCookieOf(response)).isNotBlank().isNotEqualTo(first);
  }

  @Test
  void webRefreshWithoutCookieIsRejected() throws Exception {
    var response =
        mockMvc
            .post()
            .uri("/api/auth/refresh")
            .header("X-Client", "web")
            .accept(MediaType.APPLICATION_JSON)
            .exchange();

    assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(response).bodyJson().extractingPath("$.error").isEqualTo("invalid_refresh_token");
  }

  private MvcTestResult webLogin(String email) {
    return mockMvc
        .post()
        .uri("/api/auth/login")
        .header("X-Client", "web")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(JSON.writeValueAsString(Map.of("email", email, "password", PASSWORD)))
        .exchange();
  }

  private MvcTestResult webRefresh(String refreshCookie) {
    return mockMvc
        .post()
        .uri("/api/auth/refresh")
        .header("X-Client", "web")
        .cookie(new Cookie("refresh_token", refreshCookie))
        .accept(MediaType.APPLICATION_JSON)
        .exchange();
  }

  private String refreshCookieOf(MvcTestResult response) {
    var setCookie = response.getResponse().getHeader("Set-Cookie");
    return setCookie.substring("refresh_token=".length(), setCookie.indexOf(';'));
  }

  private static String sha256(String token) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
  }

  private void registerAccount(String email) {
    mockMvc
        .post()
        .uri("/graphql")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(registerBody(email, PASSWORD))
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
        .header("X-Client", "mobile")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(JSON.writeValueAsString(body))
        .exchange();
  }
}
