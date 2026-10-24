package com.matchmap.api.health;

import javax.sql.DataSource;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.jdbc.health.DataSourceHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class HealthConfiguration {

  @Bean
  HealthIndicator applicationHealthIndicator() {
    return () -> Health.up().build();
  }

  @Bean
  HealthIndicator databaseHealthIndicator(DataSource dataSource) {
    return new DataSourceHealthIndicator(dataSource);
  }
}
