package com.matchmap.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "MATCHMAP_CORS_ALLOWED_ORIGINS=https://web.example.test")
@AutoConfigureMockMvc
class CorsIT {

  private static final String CONFIGURED_ORIGIN = "https://web.example.test";

  @Autowired MockMvcTester mockMvc;

  @Test
  void corsAllowsCredentialsFromConfiguredWebOrigin() {
    var response =
        mockMvc
            .options()
            .uri("/api/auth/login")
            .header("Origin", CONFIGURED_ORIGIN)
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "content-type,x-client")
            .exchange();

    assertThat(response).hasStatus(HttpStatus.OK);
    assertThat(response.getResponse().getHeader("Access-Control-Allow-Origin"))
        .isEqualTo(CONFIGURED_ORIGIN);
    assertThat(response.getResponse().getHeader("Access-Control-Allow-Credentials"))
        .isEqualTo("true");
    assertThat(response.getResponse().getHeader("Access-Control-Allow-Headers"))
        .containsIgnoringCase("x-client");
  }
}
