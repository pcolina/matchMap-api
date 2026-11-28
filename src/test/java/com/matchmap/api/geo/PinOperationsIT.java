package com.matchmap.api.geo;

import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
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
class PinOperationsIT {

  private static final String PASSWORD = "Correcthorse1!";
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String MADRID = "00000000-0000-0000-0000-000000000801";
  private static final String GRANADA = "00000000-0000-0000-0000-000000000802";
  private static final String RETIRED = "00000000-0000-0000-0000-000000000803";
  private static final String UNKNOWN = "00000000-0000-0000-0000-000000000999";
  private static final String SET_PIN =
      "mutation($cityId: ID!, $status: PinStatus!) { setCityPin(cityId: $cityId, status: $status)"
          + " { status city { name } } }";
  private static final String REMOVE_PIN =
      "mutation($cityId: ID!) { removeCityPin(cityId: $cityId) }";
  private static final String CITY_PINS = "{ cityPins { status city { name } } }";

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
            + " ('00000000-0000-0000-0000-000000000800', 'ES', 'Spain')");
    jdbcTemplate.update(
        "INSERT INTO city (id, city_key, country_id, name, latitude, longitude) VALUES"
            + " ('"
            + MADRID
            + "', 'es-madrid', '00000000-0000-0000-0000-000000000800',"
            + " 'Madrid', 40.416800, -3.703800),"
            + " ('"
            + GRANADA
            + "', 'es-granada', '00000000-0000-0000-0000-000000000800',"
            + " 'Granada', 37.176400, -3.598600)");
    jdbcTemplate.update(
        "INSERT INTO city (id, city_key, country_id, name, latitude, longitude, active) VALUES"
            + " ('"
            + RETIRED
            + "', 'es-old', '00000000-0000-0000-0000-000000000800',"
            + " 'Old Town', 40.000000, -3.000000, false)");
  }

  @Test
  @DisplayName("Pin a city as visited")
  void pinningACityAsVisitedStoresOneVisitedPin() throws Exception {
    String token = newUser();

    var response = graphql(SET_PIN, Map.of("cityId", MADRID, "status", "VISITED"), token);

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(data(response).at("/setCityPin/status").asString()).isEqualTo("VISITED");
    assertThat(pinCount(MADRID)).isEqualTo(1);
    assertThat(pinStatus(MADRID)).isEqualTo("VISITED");
  }

  @Test
  @DisplayName("Pinning again replaces the status")
  void pinningAgainReplacesTheStatusAndKeepsOneRow() throws Exception {
    String token = newUser();
    graphql(SET_PIN, Map.of("cityId", MADRID, "status", "WANT_TO_GO"), token);

    graphql(SET_PIN, Map.of("cityId", MADRID, "status", "LIVED"), token);

    assertThat(pinCount(MADRID)).isEqualTo(1);
    assertThat(pinStatus(MADRID)).isEqualTo("LIVED");
  }

  @Test
  @DisplayName("Pins are not shared between users")
  void pinsAreNotSharedBetweenUsers() throws Exception {
    String ana = newUser();
    String bob = newUser();
    graphql(SET_PIN, Map.of("cityId", MADRID, "status", "VISITED"), ana);

    var response = graphql(CITY_PINS, null, bob);

    assertThat(data(response).at("/cityPins").size()).isZero();
  }

  @Test
  @DisplayName("Pin without a token is rejected")
  void pinWithoutATokenIsRejectedAsUnauthorized() throws Exception {
    var response = graphql(SET_PIN, Map.of("cityId", MADRID, "status", "VISITED"), null);

    assertThat(response)
        .bodyJson()
        .extractingPath("$.errors[0].extensions.classification")
        .isEqualTo("UNAUTHORIZED");
    assertThat(pinCount(MADRID)).isZero();
  }

  @Test
  @DisplayName("Pin of a city that does not exist is rejected")
  void pinOfACityThatDoesNotExistIsRejected() throws Exception {
    String token = newUser();

    var response = graphql(SET_PIN, Map.of("cityId", UNKNOWN, "status", "VISITED"), token);

    assertThat(response)
        .bodyJson()
        .extractingPath("$.errors[0].extensions.code")
        .isEqualTo("VALIDATION_FAILED");
    assertThat(response)
        .bodyJson()
        .extractingPath("$.errors[0].extensions.field")
        .isEqualTo("cityId");
    assertThat(pinCount(UNKNOWN)).isZero();
  }

  @Test
  @DisplayName("Unknown status is rejected")
  void unknownStatusIsRejectedAndNothingIsStored() throws Exception {
    String token = newUser();

    var response =
        graphql(
            "mutation { setCityPin(cityId: \"" + MADRID + "\", status: PREMIUM) { status } }",
            null,
            token);

    assertThat(response).bodyJson().hasPath("$.errors");
    assertThat(pinCount(MADRID)).isZero();
  }

  @Test
  @DisplayName("Remove an existing pin")
  void removingAnExistingPinDeletesIt() throws Exception {
    String token = newUser();
    graphql(SET_PIN, Map.of("cityId", MADRID, "status", "VISITED"), token);

    var response = graphql(REMOVE_PIN, Map.of("cityId", MADRID), token);

    assertThat(data(response).at("/removeCityPin").asBoolean()).isTrue();
    assertThat(pinCount(MADRID)).isZero();
  }

  @Test
  @DisplayName("Remove a city without a pin")
  void removingACityWithoutAPinLeavesThePinsUnchanged() throws Exception {
    String token = newUser();
    graphql(SET_PIN, Map.of("cityId", MADRID, "status", "VISITED"), token);

    graphql(REMOVE_PIN, Map.of("cityId", GRANADA), token);

    assertThat(pinCount(MADRID)).isEqualTo(1);
    assertThat(pinCount(GRANADA)).isZero();
  }

  @Test
  @DisplayName("Retired city rejects new pins")
  void pinOfARetiredCityIsRejectedAndWritesNothing() throws Exception {
    String token = newUser();

    var response = graphql(SET_PIN, Map.of("cityId", RETIRED, "status", "VISITED"), token);

    assertThat(response).bodyJson().hasPath("$.errors");
    assertThat(pinCount(RETIRED)).isZero();
  }

  @Test
  @DisplayName("Read own pins")
  void cityPinsReturnsOwnPinsWithTheirStatuses() throws Exception {
    String token = newUser();
    graphql(SET_PIN, Map.of("cityId", MADRID, "status", "VISITED"), token);
    graphql(SET_PIN, Map.of("cityId", GRANADA, "status", "WANT_TO_GO"), token);

    var response = graphql(CITY_PINS, null, token);

    assertThat(data(response).at("/cityPins").size()).isEqualTo(2);
  }

  @Test
  @DisplayName("No pins for a new user")
  void newUserReadsAnEmptyList() throws Exception {
    String token = newUser();

    var response = graphql(CITY_PINS, null, token);

    assertThat(data(response).at("/cityPins").size()).isZero();
  }

  @Test
  @DisplayName("Pins are not readable without a token")
  void cityPinsWithoutATokenIsRejectedAsUnauthorized() throws Exception {
    var response = graphql(CITY_PINS, null, null);

    assertThat(response)
        .bodyJson()
        .extractingPath("$.errors[0].extensions.classification")
        .isEqualTo("UNAUTHORIZED");
  }

  private String newUser() throws Exception {
    String email = "pin" + uniqueSuffix() + "@example.com";
    graphql(
        "mutation { register(input: { email: \""
            + email
            + "\", password: \""
            + PASSWORD
            + "\" }) { id } }",
        null,
        null);
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

  private MvcTestResult graphql(String query, Map<String, Object> variables, String token)
      throws Exception {
    Map<String, Object> body =
        variables == null ? Map.of("query", query) : Map.of("query", query, "variables", variables);
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

  private JsonNode data(MvcTestResult response) throws Exception {
    return JSON.readTree(response.getResponse().getContentAsString()).at("/data");
  }

  private int pinCount(String cityId) {
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM user_city_pin WHERE city_id = ?::uuid", Integer.class, cityId);
  }

  private String pinStatus(String cityId) {
    return jdbcTemplate.queryForObject(
        "SELECT status FROM user_city_pin WHERE city_id = ?::uuid", String.class, cityId);
  }
}
