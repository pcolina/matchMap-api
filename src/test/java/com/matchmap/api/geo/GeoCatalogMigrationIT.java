package com.matchmap.api.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.matchmap.api.TestcontainersConfiguration;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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

  @Test
  void cityWithoutLatitudeIsRejected() throws Exception {
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
                        "INSERT INTO city (id, country_id, name, latitude, longitude) VALUES"
                            + " ('00000000-0000-0000-0000-000000000203',"
                            + " '00000000-0000-0000-0000-000000000101', 'Madrid', NULL,"
                            + " -3.703800)"))
            .isInstanceOf(SQLException.class);
      }
    }
  }

  @Test
  void cityLatitudeOutOfRangeIsRejected() throws Exception {
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
                        "INSERT INTO city (id, country_id, name, latitude, longitude) VALUES"
                            + " ('00000000-0000-0000-0000-000000000204',"
                            + " '00000000-0000-0000-0000-000000000101', 'Nowhere', 91.000000,"
                            + " 0.000000)"))
            .isInstanceOf(SQLException.class);
      }
    }
  }

  @Test
  void cityLongitudeOutOfRangeIsRejected() throws Exception {
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
                        "INSERT INTO city (id, country_id, name, latitude, longitude) VALUES"
                            + " ('00000000-0000-0000-0000-000000000205',"
                            + " '00000000-0000-0000-0000-000000000101', 'Nowhere', 0.000000,"
                            + " 181.000000)"))
            .isInstanceOf(SQLException.class);
      }
    }
  }

  @Test
  void retiredCityKeepsItsRow() throws Exception {
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
            "INSERT INTO city (id, country_id, name, latitude, longitude, active) VALUES"
                + " ('00000000-0000-0000-0000-000000000206',"
                + " '00000000-0000-0000-0000-000000000101', 'Madrid', 40.416800, -3.703800,"
                + " false)");

        try (ResultSet rows =
            statement.executeQuery("SELECT count(*) FROM city WHERE active = false")) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getInt(1)).isEqualTo(1);
        }
      }
    }
  }

  @Test
  void countryCannotBeItsOwnSuccessor() throws Exception {
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
                        "UPDATE country SET successor_id = '00000000-0000-0000-0000-000000000101'"
                            + " WHERE id = '00000000-0000-0000-0000-000000000101'"))
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("ck_country_successor_not_self");
      }
    }
  }

  @Test
  void missingSuccessorIsRejected() throws Exception {
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
                        "UPDATE country SET successor_id = '00000000-0000-0000-0000-000000000999'"
                            + " WHERE id = '00000000-0000-0000-0000-000000000101'"))
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("fk_country_successor");
      }
    }
  }
}
