package com.matchmap.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

class FirstVisitMigrationIT {

  @Test
  void existingAccountsStartTrue() throws Exception {
    try (PostgreSQLContainer postgres =
        new PostgreSQLContainer(TestcontainersConfiguration.POSTGIS_IMAGE)) {
      postgres.start();
      String url = postgres.getJdbcUrl();
      String user = postgres.getUsername();
      String password = postgres.getPassword();

      Flyway.configure()
          .dataSource(url, user, password)
          .target(MigrationVersion.fromVersion("5"))
          .load()
          .migrate();

      try (Connection connection = DriverManager.getConnection(url, user, password);
          Statement statement = connection.createStatement()) {
        statement.executeUpdate(
            "INSERT INTO user_account (id, email, username, created_at) VALUES ("
                + "'00000000-0000-0000-0000-000000000001', 'ana@example.com', 'ana', now())");
      }

      Flyway.configure().dataSource(url, user, password).load().migrate();

      try (Connection connection = DriverManager.getConnection(url, user, password);
          Statement statement = connection.createStatement();
          ResultSet rows =
              statement.executeQuery(
                  "SELECT first_visit FROM user_account WHERE email = 'ana@example.com'")) {
        assertThat(rows.next()).isTrue();
        assertThat(rows.getBoolean("first_visit")).isTrue();
      }
    }
  }
}
