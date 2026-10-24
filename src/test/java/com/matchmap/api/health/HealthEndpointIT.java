package com.matchmap.api.health;

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
@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointIT {

  @Autowired MockMvcTester mockMvc;

  @Test
  void reportsUpWhenDatabaseAvailable() {
    var response = mockMvc.get().uri("/actuator/health").exchange();

    assertThat(response).hasStatus(HttpStatus.OK);
    assertThat(response).bodyJson().extractingPath("$.status").isEqualTo("UP");
    assertThat(response)
        .bodyJson()
        .extractingPath("$.components.application.status")
        .isEqualTo("UP");
    assertThat(response).bodyJson().extractingPath("$.components.database.status").isEqualTo("UP");
  }
}
