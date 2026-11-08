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
