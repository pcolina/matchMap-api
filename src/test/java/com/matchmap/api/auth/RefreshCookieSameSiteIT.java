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
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "MATCHMAP_REFRESH_COOKIE_SAMESITE=None")
@AutoConfigureMockMvc
class RefreshCookieSameSiteIT {

  private static final String PASSWORD = "correcthorse1";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvcTester mockMvc;

  @Test
  void configuredValueIsUsed() throws Exception {
    String suffix = uniqueSuffix();
    String email = "nina" + suffix + "@example.com";
    mockMvc
        .post()
        .uri("/graphql")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(registerBody(email, "nina_" + suffix, "Nina", PASSWORD))
        .exchange();

    var response =
        mockMvc
            .post()
            .uri("/api/auth/login")
            .header("X-Client", "web")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(JSON.writeValueAsString(Map.of("email", email, "password", PASSWORD)))
            .exchange();

    assertThat(response).hasStatus(HttpStatus.OK);
    assertThat(String.join(";", response.getResponse().getHeaders("Set-Cookie")))
        .contains("SameSite=None");
  }
}
