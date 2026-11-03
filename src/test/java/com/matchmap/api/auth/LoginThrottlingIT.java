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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class LoginThrottlingIT {

  private static final String PASSWORD = "correct horse battery";
  private static final String WRONG_PASSWORD = "wrong password here";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvcTester mockMvc;

  @Test
  void throttlesAfterFailedAttempts() {
    String suffix = uniqueSuffix();
    String email = "dan" + suffix + "@example.com";
    registerAccount(email, "dan_" + suffix);
    for (int attempt = 0; attempt < 5; attempt++) {
      assertThat(login(email, WRONG_PASSWORD)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    var response = login(email, PASSWORD);

    assertThat(response).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(response).bodyJson().extractingPath("$.error").isEqualTo("too_many_attempts");
    assertThat(response.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isNotBlank();
  }

  @Test
  void otherEmailsAreNotAffected() {
    String suffix = uniqueSuffix();
    String throttled = "eve" + suffix + "@example.com";
    String other = "fay" + suffix + "@example.com";
    registerAccount(throttled, "eve_" + suffix);
    registerAccount(other, "fay_" + suffix);
    for (int attempt = 0; attempt < 5; attempt++) {
      login(throttled, WRONG_PASSWORD);
    }
    assertThat(login(throttled, PASSWORD)).hasStatus(HttpStatus.TOO_MANY_REQUESTS);

    var response = login(other, PASSWORD);

    assertThat(response).hasStatus(HttpStatus.OK);
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
