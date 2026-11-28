package com.matchmap.api.geo;

import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
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
class CountryPinStatusIT {

  private static final String PASSWORD = "Correcthorse1!";
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String COUNTRIES =
      "{ countries { code status visitedCities { name latitude longitude } } }";
  private static final String MADRID = "00000000-0000-0000-0000-000000000901";
  private static final String GRANADA = "00000000-0000-0000-0000-000000000902";
  private static final String NEW_YORK = "00000000-0000-0000-0000-000000000903";
  private static final String DENVER = "00000000-0000-0000-0000-000000000904";
  private static final String SAN_FRANCISCO = "00000000-0000-0000-0000-000000000905";

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

  @BeforeEach
  void loadCatalog() {
    jdbcTemplate.update("DELETE FROM user_city_pin");
    jdbcTemplate.update("DELETE FROM city");
    jdbcTemplate.update("DELETE FROM country_name");
    jdbcTemplate.update("DELETE FROM country");
    jdbcTemplate.update(
        "INSERT INTO country (id, code, name) VALUES"
            + " ('00000000-0000-0000-0000-000000000910', 'ES', 'Spain'),"
            + " ('00000000-0000-0000-0000-000000000911', 'US', 'United States'),"
            + " ('00000000-0000-0000-0000-000000000912', 'FR', 'France')");
    jdbcTemplate.update(
        "INSERT INTO city (id, city_key, country_id, name, latitude, longitude) VALUES"
            + " ('"
            + MADRID
            + "', 'es-madrid', '00000000-0000-0000-0000-000000000910', 'Madrid', 40.416800, -3.703800),"
            + " ('"
            + GRANADA
            + "', 'es-granada', '00000000-0000-0000-0000-000000000910', 'Granada', 37.176400, -3.598600),"
            + " ('"
            + NEW_YORK
            + "', 'us-new-york', '00000000-0000-0000-0000-000000000911', 'New York', 40.712800, -74.006000),"
            + " ('"
            + DENVER
            + "', 'us-denver', '00000000-0000-0000-0000-000000000911', 'Denver', 39.739200, -104.990300),"
            + " ('"
            + SAN_FRANCISCO
            + "', 'us-san-francisco', '00000000-0000-0000-0000-000000000911', 'San Francisco', 37.774900, -122.419400)");
  }

  @Test
  @DisplayName("Lived city makes the country lived")
  void livedCityMakesTheCountryLived() throws Exception {
    String token = newUser("lived");
    pin(token, MADRID, "LIVED");
    pin(token, GRANADA, "WANT_TO_GO");

    assertThat(statusOf("ES", token)).isEqualTo("LIVED");
  }

  @Test
  @DisplayName("Visited city makes the country visited")
  void visitedCityMakesTheCountryVisited() throws Exception {
    String token = newUser("visited");
    pin(token, NEW_YORK, "VISITED");

    assertThat(statusOf("US", token)).isEqualTo("VISITED");
  }

  @Test
  @DisplayName("Only wanted cities make the country want to go")
  void wantToGoCityMakesTheCountryWantToGo() throws Exception {
    String token = newUser("wish");
    pin(token, SAN_FRANCISCO, "WANT_TO_GO");

    assertThat(statusOf("US", token)).isEqualTo("WANT_TO_GO");
  }

  @Test
  @DisplayName("Lived beats visited in the same country")
  void livedBeatsVisitedInTheSameCountry() throws Exception {
    String token = newUser("priority");
    pin(token, NEW_YORK, "VISITED");
    pin(token, DENVER, "LIVED");

    assertThat(statusOf("US", token)).isEqualTo("LIVED");
  }

  @Test
  @DisplayName("Country without pins has no status")
  void countryWithoutPinsHasNoStatus() throws Exception {
    String token = newUser("none");

    assertThat(statusOf("FR", token)).isNull();
  }

  @Test
  @DisplayName("Wanted cities are not in the visited list")
  void visitedCitiesOfSpainAreMadridOnly() throws Exception {
    String token = newUser("visitedlist");
    pin(token, MADRID, "VISITED");
    pin(token, GRANADA, "WANT_TO_GO");

    assertThat(visitedCityNames("ES", token)).containsExactly("Madrid");
  }

  @Test
  @DisplayName("Visited cities come with coordinates")
  void visitedCitiesCarryTheirCoordinates() throws Exception {
    String token = newUser("coordinates");
    pin(token, MADRID, "LIVED");

    JsonNode city = visitedCities("ES", token).get(0);
    assertThat(city.at("/latitude").asDouble()).isEqualTo(40.4168);
    assertThat(city.at("/longitude").asDouble()).isEqualTo(-3.7038);
  }

  private String newUser(String name) throws Exception {
    String email = name + uniqueSuffix() + "@example.com";
    graphql(
        "mutation { register(input: { email: \""
            + email
            + "\", password: \""
            + PASSWORD
            + "\" }) { id } }",
        null);
    String id =
        jdbcTemplate.queryForObject(
            "SELECT id::text FROM user_account WHERE email = ?", String.class, email);
    return login(email, id);
  }

  private String login(String email, String id) throws Exception {
    var response =
        mockMvc
            .post()
            .uri("/api/auth/login")
            .header("X-Client", "mobile")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(JSON.writeValueAsString(Map.of("email", email, "password", PASSWORD)))
            .exchange();
    return JSON.readTree(response.getResponse().getContentAsString()).get("accessToken").asString();
  }

  private void pin(String token, String cityId, String status) throws Exception {
    String userId =
        jdbcTemplate.queryForObject(
            "SELECT id::text FROM user_account WHERE email = ?", String.class, emailOf(token));
    jdbcTemplate.update(
        "INSERT INTO user_city_pin (user_id, city_id, status, created_at, updated_at)"
            + " VALUES (?::uuid, ?::uuid, ?, now(), now())",
        userId,
        cityId,
        status);
  }

  private String emailOf(String token) throws Exception {
    var response = graphql("{ me { email } }", token);
    return JSON.readTree(response.getResponse().getContentAsString())
        .at("/data/me/email")
        .asString();
  }

  private String statusOf(String code, String token) throws Exception {
    JsonNode country = countryNode(code, token);
    JsonNode status = country.at("/status");
    return status.isNull() ? null : status.asString();
  }

  private List<String> visitedCityNames(String code, String token) throws Exception {
    List<String> names = new ArrayList<>();
    visitedCities(code, token).forEach(city -> names.add(city.at("/name").asString()));
    return names;
  }

  private JsonNode visitedCities(String code, String token) throws Exception {
    return countryNode(code, token).at("/visitedCities");
  }

  private JsonNode countryNode(String code, String token) throws Exception {
    var response = graphql(COUNTRIES, token);
    JsonNode countries =
        JSON.readTree(response.getResponse().getContentAsString()).at("/data/countries");
    JsonNode found = null;
    for (JsonNode country : countries) {
      if (code.equals(country.at("/code").asString())) {
        found = country;
      }
    }
    assertThat(found).isNotNull();
    return found;
  }

  private MvcTestResult graphql(String query, String token) throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("query", query);
    var request =
        mockMvc
            .post()
            .uri("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON);
    if (token != null) {
      request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
    return request.content(JSON.writeValueAsString(body)).exchange();
  }
}
