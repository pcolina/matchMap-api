package com.matchmap.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  public static final DockerImageName POSTGIS_IMAGE =
      DockerImageName.parse("postgis/postgis:17-3.5").asCompatibleSubstituteFor("postgres");

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer(POSTGIS_IMAGE);
  }
}
