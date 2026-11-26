package com.matchmap.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

class ProfileMigrationIT {

  @Test
  void existingAccountsGetFreeSubscriptionAndEmptyProfile() throws Exception {
    try (PostgreSQLContainer postgres =
        new PostgreSQLContainer(TestcontainersConfiguration.POSTGIS_IMAGE)) {
      postgres.start();
      String url = postgres.getJdbcUrl();
      String user = postgres.getUsername();
      String password = postgres.getPassword();

      Flyway.configure()
          .dataSource(url, user, password)
          .target(MigrationVersion.fromVersion("6"))
          .load()
          .migrate();

      try (Connection connection = DriverManager.getConnection(url, user, password);
          Statement statement = connection.createStatement()) {
        statement.executeUpdate(
            "INSERT INTO user_account (id, email, username, created_at) VALUES ("
                + "'00000000-0000-0000-0000-000000000002', 'bea@example.com', 'bea', now())");
      }

      Flyway.configure().dataSource(url, user, password).load().migrate();

      try (Connection connection = DriverManager.getConnection(url, user, password);
          Statement statement = connection.createStatement();
          ResultSet rows =
              statement.executeQuery(
                  "SELECT subscription, first_name, last_name, nationality, birth_country"
                      + " FROM user_account WHERE email = 'bea@example.com'")) {
        assertThat(rows.next()).isTrue();
        assertThat(rows.getString("subscription")).isEqualTo("FREE");
        assertThat(rows.getString("first_name")).isNull();
        assertThat(rows.getString("last_name")).isNull();
        assertThat(rows.getString("nationality")).isNull();
        assertThat(rows.getString("birth_country")).isNull();
      }
    }
  }

  @Test
  void rejectsInvalidPlanAndCountryCodes() throws Exception {
    try (PostgreSQLContainer postgres =
        new PostgreSQLContainer(TestcontainersConfiguration.POSTGIS_IMAGE)) {
      postgres.start();
      String url = postgres.getJdbcUrl();
      String user = postgres.getUsername();
      String password = postgres.getPassword();

      Flyway.configure().dataSource(url, user, password).load().migrate();

      try (Connection connection = DriverManager.getConnection(url, user, password);
          Statement statement = connection.createStatement()) {
        statement.executeUpdate(
            "INSERT INTO user_account (id, email, username, created_at, nationality, birth_country)"
                + " VALUES ('00000000-0000-0000-0000-000000000003', 'cai@example.com', 'cai',"
                + " now(), 'ES', 'FR')");

        assertThatThrownBy(
                () ->
                    statement.executeUpdate(
                        "UPDATE user_account SET subscription = 'PREMIUM'"
                            + " WHERE email = 'cai@example.com'"))
            .isInstanceOf(SQLException.class);
        assertThatThrownBy(
                () ->
                    statement.executeUpdate(
                        "UPDATE user_account SET nationality = 'es'"
                            + " WHERE email = 'cai@example.com'"))
            .isInstanceOf(SQLException.class);
        assertThatThrownBy(
                () ->
                    statement.executeUpdate(
                        "UPDATE user_account SET birth_country = 'Spain'"
                            + " WHERE email = 'cai@example.com'"))
            .isInstanceOf(SQLException.class);
      }
    }
  }
}
