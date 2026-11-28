package com.matchmap.api.geo;

import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import java.util.Map;
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
class CatalogImportGraphqlIT {

  private static final String PASSWORD = "Correcthorse1!";
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String MUTATION =
      "mutation Import($input: CatalogImportInput!) { importCatalog(input: $input) {"
          + " countriesLoaded namesLoaded citiesLoaded missingNames"
          + " errors { file line reason } } }";

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void adminImportsTheCatalogOverGraphql() throws Exception {
    String email = "importer" + uniqueSuffix() + "@example.com";
    register(email);
    jdbcTemplate.update("UPDATE user_account SET role = 'ADMIN' WHERE email = ?", email);
    String token = login(email);

    var response =
        graphql(
            Map.of(
                "query",
                MUTATION,
                "variables",
                Map.of(
                    "input",
                    Map.of(
                        "countries", "code,name\nES,Spain\n",
                        "countryNames", "code,lang,name\nES,es,España\n",
                        "cities",
                            "city_key,country_code,name,latitude,longitude\nes-madrid,ES,Madrid,40.4168,-3.7038\n"))),
            token);

    JsonNode result = JSON.readTree(response.getResponse().getContentAsString());
    assertThat(result.has("errors")).isFalse();
    assertThat(result.at("/data/importCatalog/countriesLoaded").asInt()).isEqualTo(1);
    assertThat(result.at("/data/importCatalog/citiesLoaded").asInt()).isEqualTo(1);
    assertThat(result.at("/data/importCatalog/errors").size()).isZero();
  }

  private void register(String email) throws Exception {
    graphql(
        Map.of(
            "query",
            "mutation { register(input: { email: \""
                + email
                + "\", password: \""
                + PASSWORD
                + "\" }) { id } }"),
        null);
  }

  private String login(String email) throws Exception {
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

  private MvcTestResult graphql(Map<String, Object> body, String accessToken) throws Exception {
    var request =
        mockMvc
            .post()
            .uri("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON);
    if (accessToken != null) {
      request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }
    return request.content(JSON.writeValueAsString(body)).exchange();
  }
}
