package com.matchmap.api.geo;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.matchmap.api.TestcontainersConfiguration;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

class UserCityPinMigrationIT {

  private static final String USER = "00000000-0000-0000-0000-000000000701";
  private static final String COUNTRY = "00000000-0000-0000-0000-000000000702";
  private static final String CITY = "00000000-0000-0000-0000-000000000703";

  @Test
  void pinWithUnknownStatusIsRejected() throws Exception {
    try (PostgreSQLContainer postgres =
        new PostgreSQLContainer(TestcontainersConfiguration.POSTGIS_IMAGE)) {
      postgres.start();
      String url = postgres.getJdbcUrl();
      String user = postgres.getUsername();
      String password = postgres.getPassword();
      Flyway.configure().dataSource(url, user, password).load().migrate();

      try (Connection connection = DriverManager.getConnection(url, user, password);
          Statement statement = connection.createStatement()) {
        insertCatalogAndAccount(statement);

        assertThatThrownBy(
                () ->
                    statement.executeUpdate(
                        "INSERT INTO user_city_pin (user_id, city_id, status, created_at,"
                            + " updated_at) VALUES ('"
                            + USER
                            + "', '"
                            + CITY
                            + "', 'PREMIUM', now(), now())"))
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("ck_user_city_pin_status");
      }
    }
  }

  @Test
  void secondPinOnTheSameUserAndCityIsRejected() throws Exception {
    try (PostgreSQLContainer postgres =
        new PostgreSQLContainer(TestcontainersConfiguration.POSTGIS_IMAGE)) {
      postgres.start();
      String url = postgres.getJdbcUrl();
      String user = postgres.getUsername();
      String password = postgres.getPassword();
      Flyway.configure().dataSource(url, user, password).load().migrate();

      try (Connection connection = DriverManager.getConnection(url, user, password);
          Statement statement = connection.createStatement()) {
        insertCatalogAndAccount(statement);
        statement.executeUpdate(
            "INSERT INTO user_city_pin (user_id, city_id, status, created_at, updated_at)"
                + " VALUES ('"
                + USER
                + "', '"
                + CITY
                + "', 'VISITED', now(), now())");

        assertThatThrownBy(
                () ->
                    statement.executeUpdate(
                        "INSERT INTO user_city_pin (user_id, city_id, status, created_at,"
                            + " updated_at) VALUES ('"
                            + USER
                            + "', '"
                            + CITY
                            + "', 'LIVED', now(), now())"))
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("pk_user_city_pin");
      }
    }
  }

  private static void insertCatalogAndAccount(Statement statement) throws SQLException {
    statement.executeUpdate(
        "INSERT INTO country (id, code, name) VALUES ('" + COUNTRY + "', 'ES', 'Spain')");
    statement.executeUpdate(
        "INSERT INTO city (id, city_key, country_id, name, latitude, longitude) VALUES ('"
            + CITY
            + "', 'es-madrid', '"
            + COUNTRY
            + "', 'Madrid', 40.416800, -3.703800)");
    statement.executeUpdate(
        "INSERT INTO user_account (id, email, username, created_at, first_visit, subscription, role)"
            + " VALUES ('"
            + USER
            + "', 'ana@example.com', 'ana', now(), true, 'FREE', 'USER')");
  }
}
