package com.matchmap.api.auth;

import static com.matchmap.api.support.GraphqlRequests.registerBody;
import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
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
class ClientTypeHeaderIT {

  private static final String PASSWORD = "Correcthorse1!";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvcTester mockMvc;

  @Test
  void unknownClientTypeIsRejectedAndTokenIsNotExchanged() throws Exception {
    String suffix = uniqueSuffix();
    String email = "vera" + suffix + "@example.com";
    registerAccount(email);
    var refreshToken = refreshTokenOf(login(email));

    var rejected = post("/api/auth/refresh", "desktop", Map.of("refreshToken", refreshToken));
    var stillValid = post("/api/auth/refresh", "mobile", Map.of("refreshToken", refreshToken));

    assertThat(rejected).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(rejected).bodyJson().extractingPath("$.error").isEqualTo("invalid_client");
    assertThat(stillValid).hasStatus(HttpStatus.OK);
  }

  @Test
  void mobileSignInReturnsRefreshTokenInBody() throws Exception {
    String suffix = uniqueSuffix();
    String email = "mia" + suffix + "@example.com";
    registerAccount(email);

    var response = login(email);

    assertThat(response).hasStatus(HttpStatus.OK);
    assertThat(response).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
    assertThat(response).bodyJson().extractingPath("$.accessToken").asString().isNotBlank();
    assertThat(response).bodyJson().extractingPath("$.expiresIn").isEqualTo(900);
    assertThat(refreshTokenOf(response)).isNotBlank();
  }

  @Test
  void webSignInSetsRefreshCookieAndOmitsRefreshTokenFromBody() throws Exception {
    String suffix = uniqueSuffix();
    String email = "wendy" + suffix + "@example.com";
    registerAccount(email);

    var response = post("/api/auth/login", "web", Map.of("email", email, "password", PASSWORD));

    assertThat(response).hasStatus(HttpStatus.OK);
    assertThat(response).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
    assertThat(response).bodyJson().extractingPath("$.accessToken").asString().isNotBlank();
    assertThat(response).bodyJson().extractingPath("$.expiresIn").isNotNull();
    assertThat(JSON.readTree(response.getResponse().getContentAsString()).has("refreshToken"))
        .isFalse();
    assertThat(setCookieOf(response))
        .contains("refresh_token=")
        .contains("HttpOnly")
        .contains("Secure")
        .contains("Path=/api/auth")
        .contains("Max-Age=2592000");
  }

  @Test
  void missingClientTypeOnLoginIsRejectedAndIssuesNoTokens() throws Exception {
    String suffix = uniqueSuffix();
    String email = "lena" + suffix + "@example.com";
    registerAccount(email);

    var response =
        mockMvc
            .post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(JSON.writeValueAsString(Map.of("email", email, "password", PASSWORD)))
            .exchange();

    assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(response).bodyJson().extractingPath("$.error").isEqualTo("invalid_client");
    assertThat(response.getResponse().getContentAsString()).doesNotContain("accessToken");
    assertThat(response.getResponse().getHeader("Set-Cookie")).isNull();
  }

  @Test
  void missingClientTypeOnRefreshIsRejected() throws Exception {
    String suffix = uniqueSuffix();
    String email = "wade" + suffix + "@example.com";
    registerAccount(email);
    var refreshToken = refreshTokenOf(login(email));

    var response =
        mockMvc
            .post()
            .uri("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(JSON.writeValueAsString(Map.of("refreshToken", refreshToken)))
            .exchange();

    assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(response).bodyJson().extractingPath("$.error").isEqualTo("invalid_client");
  }

  @Test
  void webSignInDefaultsSameSiteToLax() throws Exception {
    String suffix = uniqueSuffix();
    String email = "zoe" + suffix + "@example.com";
    registerAccount(email);

    var response = post("/api/auth/login", "web", Map.of("email", email, "password", PASSWORD));

    assertThat(response).hasStatus(HttpStatus.OK);
    assertThat(setCookieOf(response)).contains("SameSite=Lax");
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
    return post("/api/auth/login", "mobile", Map.of("email", email, "password", PASSWORD));
  }

  private String refreshTokenOf(MvcTestResult response) throws Exception {
    return JSON.readTree(response.getResponse().getContentAsString())
        .get("refreshToken")
        .asString();
  }

  private String setCookieOf(MvcTestResult response) {
    return String.join(";", response.getResponse().getHeaders("Set-Cookie"));
  }

  private MvcTestResult post(String uri, String clientType, Map<String, String> body) {
    return mockMvc
        .post()
        .uri(uri)
        .header("X-Client", clientType)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(JSON.writeValueAsString(body))
        .exchange();
  }
}
