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

  private static final String PASSWORD = "correcthorse1";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvcTester mockMvc;

  @Test
  void unknownClientTypeIsRejectedAndTokenIsNotExchanged() throws Exception {
    String suffix = uniqueSuffix();
    String email = "vera" + suffix + "@example.com";
    registerAccount(email, "vera_" + suffix);
    var refreshToken = refreshTokenOf(login(email));

    var rejected = post("/api/auth/refresh", "desktop", Map.of("refreshToken", refreshToken));
    var stillValid = post("/api/auth/refresh", "mobile", Map.of("refreshToken", refreshToken));

    assertThat(rejected).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(rejected).bodyJson().extractingPath("$.error").isEqualTo("invalid_client");
    assertThat(stillValid).hasStatus(HttpStatus.OK);
  }

  @Test
  void missingClientTypeOnRefreshIsRejected() throws Exception {
    String suffix = uniqueSuffix();
    String email = "wade" + suffix + "@example.com";
    registerAccount(email, "wade_" + suffix);
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

  private void registerAccount(String email, String username) {
    mockMvc
        .post()
        .uri("/graphql")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(registerBody(email, username, "Vera", PASSWORD))
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
