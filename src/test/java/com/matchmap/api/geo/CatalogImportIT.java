package com.matchmap.api.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.matchmap.api.TestcontainersConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CatalogImportIT {

  private static final String COUNTRIES =
      """
      code,name
      ES,Spain
      FR,France
      """;

  private static final String COUNTRY_NAMES =
      """
      code,lang,name
      ES,es,EspaÃ±a
      FR,es,Francia
      """;

  private static final String CITIES =
      """
      city_key,country_code,name,latitude,longitude
      es-madrid,ES,Madrid,40.4168,-3.7038
      fr-paris,FR,Paris,48.8566,2.3522
      """;

  @Autowired CatalogImportService importService;

  @Autowired JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearCatalog() {
    jdbcTemplate.update("DELETE FROM user_city_pin");
    jdbcTemplate.update("DELETE FROM city");
    jdbcTemplate.update("DELETE FROM country_name");
    jdbcTemplate.update("DELETE FROM country");
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Re-running the import creates no duplicates")
  void runningTheImportTwiceLeavesTheSameCountsOfCountriesAndCities() {
    signInAs(insertAccount("ADMIN"));

    importService.importCatalog(new CatalogImportInput(COUNTRIES, COUNTRY_NAMES, CITIES));
    long countriesAfterFirst = count("country");
    long citiesAfterFirst = count("city");

    importService.importCatalog(new CatalogImportInput(COUNTRIES, COUNTRY_NAMES, CITIES));

    assertThat(count("country")).isEqualTo(countriesAfterFirst).isEqualTo(2);
    assertThat(count("city")).isEqualTo(citiesAfterFirst).isEqualTo(2);
  }

  @Test
  @DisplayName("Invalid row rejects the whole file")
  void invalidRowWritesNothingAndReportsItsLine() {
    signInAs(insertAccount("ADMIN"));
    String countriesWithLowercaseCode =
        """
        code,name
        ES,Spain
        fr,France
        """;

    CatalogImportResult result =
        importService.importCatalog(
            new CatalogImportInput(countriesWithLowercaseCode, COUNTRY_NAMES, CITIES));

    assertThat(result.errors())
        .anySatisfy(
            error -> {
              assertThat(error.file()).isEqualTo("countries");
              assertThat(error.line()).isEqualTo(3);
              assertThat(error.reason()).isNotBlank();
            });
    assertThat(count("country")).isZero();
    assertThat(count("city")).isZero();
  }

  @Test
  @DisplayName("Non-admin cannot change the catalog")
  void nonAdministratorCannotImportTheCatalog() {
    signInAs(insertAccount("USER"));

    assertThatThrownBy(
            () ->
                importService.importCatalog(
                    new CatalogImportInput(COUNTRIES, COUNTRY_NAMES, CITIES)))
        .isInstanceOf(AccessDeniedException.class);
    assertThat(count("country")).isZero();
  }

  private UUID insertAccount(String role) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO user_account (id, email, username, created_at, first_visit, subscription, role)"
            + " VALUES (?, ?, ?, now(), true, 'FREE', ?)",
        id,
        id + "@example.com",
        "u" + id.toString().substring(0, 8),
        role);
    return id;
  }

  private void signInAs(UUID accountId) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(accountId.toString(), null, List.of()));
  }

  private long count(String table) {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
  }
}
