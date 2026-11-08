package com.matchmap.api.auth;

import static com.matchmap.api.support.GraphqlRequests.registerBody;
import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
@ExtendWith(OutputCaptureExtension.class)
class CredentialLeakIT {

  private static final String PASSWORD = "correcthorse1";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void signInLeavesNoSecretsInLogs(CapturedOutput output) throws Exception {
    String suffix = uniqueSuffix();
    String email = "lee" + suffix + "@example.com";
    registerAccount(email, "lee_" + suffix);
    var login = post("/api/auth/login", Map.of("email", email, "password", PASSWORD));
    var accessToken = field(login, "accessToken");
    var refreshToken = field(login, "refreshToken");

    var refreshed = post("/api/auth/refresh", Map.of("refreshToken", refreshToken));

    assertThat(refreshed).hasStatus(HttpStatus.OK);
    assertThat(output.getAll())
        .doesNotContain(PASSWORD)
        .doesNotContain(accessToken)
        .doesNotContain(refreshToken)
        .doesNotContain(field(refreshed, "accessToken"))
        .doesNotContain(field(refreshed, "refreshToken"));
  }

  @Test
  void storesOnlyRefreshTokenHashes() throws Exception {
    String suffix = uniqueSuffix();
    String email = "kim" + suffix + "@example.com";
    registerAccount(email, "kim_" + suffix);
    var refreshToken =
        field(
            post("/api/auth/login", Map.of("email", email, "password", PASSWORD)), "refreshToken");

    var stored =
        jdbcTemplate.queryForList(
            "SELECT token_hash FROM refresh_token"
                + " WHERE account_id = (SELECT id FROM user_account WHERE email = ?)",
            String.class,
            email);

    assertThat(stored)
        .isNotEmpty()
        .allSatisfy(
            hash -> {
              assertThat(hash).hasSize(64).isNotEqualTo(refreshToken);
              assertThat(hash).doesNotContain(refreshToken);
            });
  }

  private void registerAccount(String email, String username) {
    mockMvc
        .post()
        .uri("/graphql")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(registerBody(email, username, "Lee", PASSWORD))
        .exchange();
  }

  private String field(MvcTestResult response, String name) throws Exception {
    return JSON.readTree(response.getResponse().getContentAsString()).get(name).asString();
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
