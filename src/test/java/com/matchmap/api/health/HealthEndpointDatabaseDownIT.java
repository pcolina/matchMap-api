package com.matchmap.api.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = "spring.datasource.hikari.connection-timeout=2000")
@AutoConfigureMockMvc
@DirtiesContext
class HealthEndpointDatabaseDownIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(TestcontainersConfiguration.POSTGIS_IMAGE);

  @Autowired MockMvcTester mockMvc;

  @Test
  void reportsDatabaseDown() {
    postgres.stop();

    var response = mockMvc.get().uri("/actuator/health").exchange();

    assertThat(response).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response)
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "status": "DOWN",
              "components": {
                "application": { "status": "UP" },
                "database": { "status": "DOWN" }
              }
            }
            """);
  }
}
