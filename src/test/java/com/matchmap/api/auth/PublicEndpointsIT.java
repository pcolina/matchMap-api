package com.matchmap.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PublicEndpointsIT {

  @Autowired MockMvcTester mockMvc;

  @Test
  void publicOperationsStayPublic() {
    var health = mockMvc.get().uri("/actuator/health").exchange();
    var apiVersion =
        mockMvc
            .post()
            .uri("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content("{\"query\": \"{ apiVersion }\"}")
            .exchange();

    assertThat(health).hasStatus(HttpStatus.OK);
    assertThat(apiVersion).hasStatus(HttpStatus.OK);
    assertThat(apiVersion).bodyJson().hasPathSatisfying("$.data.apiVersion", v -> {});
  }

  @Test
  void unknownPathRequiresToken() {
    var response = mockMvc.get().uri("/api/anything").exchange();

    assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void unlistedAuthPathRequiresToken() {
    var response = mockMvc.post().uri("/api/auth/anything").exchange();

    assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
  }
}
