package com.matchmap.api.auth;

import static com.matchmap.api.support.GraphqlRequests.registerBody;
import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
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

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class LogoutIT {

  private static final String PASSWORD = "correcthorse1";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvcTester mockMvc;

  @Test
  void endsSession() throws Exception {
    String suffix = uniqueSuffix();
    String email = "lena" + suffix + "@example.com";
    registerAccount(email, "lena_" + suffix);
    var refreshToken = refreshTokenOf(login(email));

    var logout = post("/api/auth/logout", Map.of("refreshToken", refreshToken));
    var refresh = post("/api/auth/refresh", Map.of("refreshToken", refreshToken));

    assertThat(logout).hasStatus(HttpStatus.NO_CONTENT);
    assertThat(refresh).hasStatus(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void unknownTokenReturnsNoContent() {
    var response = post("/api/auth/logout", Map.of("refreshToken", "unknown-" + uniqueSuffix()));

    assertThat(response).hasStatus(HttpStatus.NO_CONTENT);
  }

  @Test
  void webSignOutClearsCookie() throws Exception {
    String suffix = uniqueSuffix();
    String email = "lola" + suffix + "@example.com";
    registerAccount(email, "lola_" + suffix);
    var cookie = refreshCookieOf(webLogin(email));

    var response = webLogout(cookie);

    assertThat(response).hasStatus(HttpStatus.NO_CONTENT);
    assertThat(response.getResponse().getHeader("Set-Cookie"))
        .startsWith("refresh_token=;")
        .contains("Max-Age=0")
        .contains("Path=/api/auth");
  }

  @Test
  void webSignOutWithoutCookieReturnsNoContent() {
    var response =
        mockMvc
            .post()
            .uri("/api/auth/logout")
            .header("X-Client", "web")
            .accept(MediaType.APPLICATION_JSON)
            .exchange();

    assertThat(response).hasStatus(HttpStatus.NO_CONTENT);
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

  private MvcTestResult webLogout(String refreshCookie) {
    return mockMvc
        .post()
        .uri("/api/auth/logout")
        .header("X-Client", "web")
        .cookie(new Cookie("refresh_token", refreshCookie))
        .accept(MediaType.APPLICATION_JSON)
        .exchange();
  }

  private String refreshCookieOf(MvcTestResult response) {
    var setCookie = response.getResponse().getHeader("Set-Cookie");
    return setCookie.substring("refresh_token=".length(), setCookie.indexOf(';'));
  }

  private void registerAccount(String email, String username) {
    mockMvc
        .post()
        .uri("/graphql")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(registerBody(email, username, "Lena", PASSWORD))
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
