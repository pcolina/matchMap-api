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

class GeoCatalogMigrationIT {

  @Test
  void cityWithoutCountryIsRejected() throws Exception {
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
            "INSERT INTO country (id, code, name) VALUES"
                + " ('00000000-0000-0000-0000-000000000101', 'ES', 'Spain')");
        statement.executeUpdate(
            "INSERT INTO city (id, country_id, name, latitude, longitude) VALUES"
                + " ('00000000-0000-0000-0000-000000000201',"
                + " '00000000-0000-0000-0000-000000000101', 'Madrid', 40.416800, -3.703800)");

        assertThatThrownBy(
                () ->
                    statement.executeUpdate(
                        "INSERT INTO city (id, country_id, name, latitude, longitude) VALUES"
                            + " ('00000000-0000-0000-0000-000000000202', NULL, 'Nowhere',"
                            + " 0.000000, 0.000000)"))
            .isInstanceOf(SQLException.class);
      }
    }
  }

  @Test
  void duplicateCountryCodeIsRejected() throws Exception {
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
            "INSERT INTO country (id, code, name) VALUES"
                + " ('00000000-0000-0000-0000-000000000101', 'ES', 'Spain')");

        assertThatThrownBy(
                () ->
                    statement.executeUpdate(
                        "INSERT INTO country (id, code, name) VALUES"
                            + " ('00000000-0000-0000-0000-000000000102', 'ES', 'Spain again')"))
            .isInstanceOf(SQLException.class);
      }
    }
  }

  @Test
  void countryCodeMustBeTwoUppercaseLetters() throws Exception {
    try (PostgreSQLContainer postgres =
        new PostgreSQLContainer(TestcontainersConfiguration.POSTGIS_IMAGE)) {
      postgres.start();
      String url = postgres.getJdbcUrl();
      String user = postgres.getUsername();
      String password = postgres.getPassword();

      Flyway.configure().dataSource(url, user, password).load().migrate();

      try (Connection connection = DriverManager.getConnection(url, user, password);
          Statement statement = connection.createStatement()) {
        assertThatThrownBy(
                () ->
                    statement.executeUpdate(
                        "INSERT INTO country (id, code, name) VALUES"
                            + " ('00000000-0000-0000-0000-000000000103', 'es', 'Spain')"))
            .isInstanceOf(SQLException.class);
        assertThatThrownBy(
                () ->
                    statement.executeUpdate(
                        "INSERT INTO country (id, code, name) VALUES"
                            + " ('00000000-0000-0000-0000-000000000104', 'SPAIN', 'Spain')"))
            .isInstanceOf(SQLException.class);
      }
    }
  }
}
