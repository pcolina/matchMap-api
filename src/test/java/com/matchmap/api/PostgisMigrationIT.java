package com.matchmap.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PostgisMigrationIT {

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void enablesPostgisExtension() {
    String appliedMigration =
        jdbcTemplate.queryForObject(
            "SELECT description FROM flyway_schema_history WHERE version = '1' AND success",
            String.class);
    String postgisVersion = jdbcTemplate.queryForObject("SELECT postgis_version()", String.class);

    assertThat(appliedMigration).isEqualTo("enable postgis");
    assertThat(postgisVersion).startsWith("3.5");
  }
}
