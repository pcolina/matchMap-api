package com.matchmap.api.geo;

import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AdminCatalogIT {

  private static final String PASSWORD = "Correcthorse1!";
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String SPAIN = "00000000-0000-0000-0000-000000001001";
  private static final String MADRID = "00000000-0000-0000-0000-000000001002";
  private static final String ANDORRA = "00000000-0000-0000-0000-000000001003";
  private static final String UNKNOWN = "00000000-0000-0000-0000-000000009999";
  private static final String CREATE_COUNTRY =
      "mutation($code: String!, $name: String!) { createCountry(code: $code, name: $name) { id code } }";
  private static final String CREATE_CITY =
      "mutation($countryId: ID!, $cityKey: String!, $name: String!, $latitude: Float!, $longitude: Float!)"
          + " { createCity(countryId: $countryId, cityKey: $cityKey, name: $name, latitude: $latitude,"
          + " longitude: $longitude) { id } }";
  private static final String RETIRE_CITY =
      "mutation($cityId: ID!) { retireCity(cityId: $cityId) { id } }";
  private static final String MERGE_COUNTRY =
      "mutation($countryId: ID!, $successorId: ID!) { mergeCountry(countryId: $countryId,"
          + " successorId: $successorId) { code } }";

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

  @BeforeEach
  void loadCatalog() {
    jdbcTemplate.update("DELETE FROM user_city_pin");
    jdbcTemplate.update("DELETE FROM city");
    jdbcTemplate.update("DELETE FROM country_name");
    jdbcTemplate.update("DELETE FROM country");
    jdbcTemplate.update(
        "INSERT INTO country (id, code, name) VALUES ('"
            + SPAIN
            + "', 'ES', 'Spain'),"
            + " ('"
            + ANDORRA
            + "', 'AD', 'Andorra')");
    jdbcTemplate.update(
        "INSERT INTO city (id, city_key, country_id, name, latitude, longitude) VALUES ('"
            + MADRID
            + "', 'es-madrid', '"
            + SPAIN
            + "', 'Madrid', 40.416800, -3.703800),"
            + " ('00000000-0000-0000-0000-000000001004', 'ad-andorra-la-vella', '"
            + ANDORRA
            + "', 'Andorra la Vella', 42.506300, 1.521800)");
  }

  @Test
  @DisplayName("Non-admin cannot change the catalog")
  void nonAdministratorCannotCreateRetireOrMerge() throws Exception {
    String user = signedInAccount(false);

    JsonNode create = call(CREATE_COUNTRY, Map.of("code", "FR", "name", "France"), user);
    JsonNode retire = call(RETIRE_CITY, Map.of("cityId", MADRID), user);
    JsonNode merge = call(MERGE_COUNTRY, Map.of("countryId", ANDORRA, "successorId", SPAIN), user);

    assertThat(create.at("/errors/0/extensions/classification").asString()).isEqualTo("FORBIDDEN");
    assertThat(retire.at("/errors/0/extensions/classification").asString()).isEqualTo("FORBIDDEN");
    assertThat(merge.at("/errors/0/extensions/classification").asString()).isEqualTo("FORBIDDEN");
    assertThat(count("SELECT count(*) FROM country WHERE code = 'FR'")).isZero();
    assertThat(count("SELECT count(*) FROM city WHERE active = false")).isZero();
    assertThat(count("SELECT count(*) FROM country WHERE successor_id IS NOT NULL")).isZero();
  }

  @Test
  void createCountryAndCityRejectInvalidRules() throws Exception {
    String admin = signedInAccount(true);

    JsonNode lowercase = call(CREATE_COUNTRY, Map.of("code", "fr", "name", "France"), admin);
    JsonNode duplicate = call(CREATE_COUNTRY, Map.of("code", "ES", "name", "Spain again"), admin);
    JsonNode latitude =
        call(
            CREATE_CITY,
            Map.of(
                "countryId",
                SPAIN,
                "cityKey",
                "es-far",
                "name",
                "Far",
                "latitude",
                91.0,
                "longitude",
                0.0),
            admin);

    assertThat(lowercase.at("/errors/0/extensions/code").asString()).isEqualTo("VALIDATION_FAILED");
    assertThat(lowercase.at("/errors/0/extensions/field").asString()).isEqualTo("code");
    assertThat(duplicate.at("/errors/0/extensions/code").asString()).isEqualTo("VALIDATION_FAILED");
    assertThat(duplicate.at("/errors/0/extensions/field").asString()).isEqualTo("code");
    assertThat(latitude.at("/errors/0/extensions/field").asString()).isEqualTo("latitude");
    assertThat(count("SELECT count(*) FROM city WHERE city_key = 'es-far'")).isZero();
  }

  @Test
  void validCountryIsCreated() throws Exception {
    String admin = signedInAccount(true);

    JsonNode created = call(CREATE_COUNTRY, Map.of("code", "FR", "name", "France"), admin);

    assertThat(created.at("/data/createCountry/code").asString()).isEqualTo("FR");
  }

  @Test
  @DisplayName("Retired city keeps its pins")
  void retiredCityKeepsItsRowAndItsPins() throws Exception {
    String admin = signedInAccount(true);
    String user = signedInAccount(false);
    pin(user, MADRID, "VISITED");

    call(RETIRE_CITY, Map.of("cityId", MADRID), admin);

    assertThat(count("SELECT count(*) FROM city WHERE id = '" + MADRID + "' AND active = false"))
        .isEqualTo(1);
    assertThat(count("SELECT count(*) FROM user_city_pin WHERE city_id = '" + MADRID + "'"))
        .isEqualTo(1);
  }

  @Test
  @DisplayName("Merged country keeps its cities")
  void mergedCountryMovesItsCitiesAndKeepsThePins() throws Exception {
    String admin = signedInAccount(true);
    String user = signedInAccount(false);
    pin(user, "00000000-0000-0000-0000-000000001004", "LIVED");

    JsonNode merged =
        call(MERGE_COUNTRY, Map.of("countryId", ANDORRA, "successorId", SPAIN), admin);

    assertThat(merged.has("errors")).isFalse();
    assertThat(
            count(
                "SELECT count(*) FROM city WHERE id = '00000000-0000-0000-0000-000000001004' AND country_id = '"
                    + SPAIN
                    + "'"))
        .isEqualTo(1);
    assertThat(
            count(
                "SELECT count(*) FROM user_city_pin WHERE city_id = '00000000-0000-0000-0000-000000001004'"))
        .isEqualTo(1);
    assertThat(
            count(
                "SELECT count(*) FROM country WHERE code = 'AD' AND successor_id = '"
                    + SPAIN
                    + "'"))
        .isEqualTo(1);
    JsonNode afterMerge = call("{ countries { code } }", Map.of(), null);
    assertThat(afterMerge.at("/data/countries").toString()).doesNotContain("\"AD\"");
  }

  @Test
  @DisplayName("Country cannot merge into itself or a missing country")
  void mergeRejectsSelfAndMissingCountries() throws Exception {
    String admin = signedInAccount(true);

    JsonNode self = call(MERGE_COUNTRY, Map.of("countryId", SPAIN, "successorId", SPAIN), admin);
    JsonNode missingSuccessor =
        call(MERGE_COUNTRY, Map.of("countryId", SPAIN, "successorId", UNKNOWN), admin);
    JsonNode missingCountry =
        call(MERGE_COUNTRY, Map.of("countryId", UNKNOWN, "successorId", SPAIN), admin);

    assertThat(self.at("/errors/0/extensions/field").asString()).isEqualTo("successorId");
    assertThat(missingSuccessor.at("/errors/0/extensions/field").asString())
        .isEqualTo("successorId");
    assertThat(missingCountry.at("/errors/0/extensions/field").asString()).isEqualTo("countryId");
    assertThat(count("SELECT count(*) FROM country WHERE successor_id IS NOT NULL")).isZero();
  }

  @Test
  void mergedCountryAcceptsNoNewCities() throws Exception {
    String admin = signedInAccount(true);
    call(MERGE_COUNTRY, Map.of("countryId", ANDORRA, "successorId", SPAIN), admin);

    JsonNode city =
        call(
            CREATE_CITY,
            Map.of(
                "countryId",
                ANDORRA,
                "cityKey",
                "ad-escaldes",
                "name",
                "Escaldes",
                "latitude",
                42.5,
                "longitude",
                1.5),
            admin);

    assertThat(city.at("/errors/0/extensions/field").asString()).isEqualTo("countryId");
  }

  @Test
  @DisplayName("Deletion operations do not exist")
  void schemaHasNoMutationThatDeletesACountryOrACity() throws Exception {
    JsonNode schema = call("{ __type(name: \"Mutation\") { fields { name } } }", Map.of(), null);

    List<String> names = new ArrayList<>();
    schema.at("/data/__type/fields").forEach(field -> names.add(field.at("/name").asString()));
    assertThat(names).noneMatch(name -> name.toLowerCase().contains("delete"));
    assertThat(names).doesNotContain("removeCountry", "removeCity", "removeCityCatalog");
  }

  private String signedInAccount(boolean admin) throws Exception {
    String email = (admin ? "admin" : "user") + uniqueSuffix() + "@example.com";
    call(
        "mutation { register(input: { email: \""
            + email
            + "\", password: \""
            + PASSWORD
            + "\" }) { id } }",
        Map.of(),
        null);
    if (admin) {
      jdbcTemplate.update("UPDATE user_account SET role = 'ADMIN' WHERE email = ?", email);
    }
    var login =
        mockMvc
            .post()
            .uri("/api/auth/login")
            .header("X-Client", "mobile")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(JSON.writeValueAsString(Map.of("email", email, "password", PASSWORD)))
            .exchange();
    return JSON.readTree(login.getResponse().getContentAsString()).get("accessToken").asString();
  }

  private void pin(String token, String cityId, String status) throws Exception {
    JsonNode me = call("{ me { id } }", Map.of(), token);
    jdbcTemplate.update(
        "INSERT INTO user_city_pin (user_id, city_id, status, created_at, updated_at)"
            + " VALUES (?::uuid, ?::uuid, ?, now(), now())",
        me.at("/data/me/id").asString(),
        cityId,
        status);
  }

  private JsonNode call(String query, Map<String, Object> variables, String token)
      throws Exception {
    Map<String, Object> body =
        variables.isEmpty()
            ? Map.of("query", query)
            : Map.of("query", query, "variables", variables);
    var request =
        mockMvc
            .post()
            .uri("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON);
    if (token != null) {
      request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
    MvcTestResult response = request.content(JSON.writeValueAsString(body)).exchange();
    return JSON.readTree(response.getResponse().getContentAsString());
  }

  private long count(String sql) {
    return jdbcTemplate.queryForObject(sql, Long.class);
  }
}
