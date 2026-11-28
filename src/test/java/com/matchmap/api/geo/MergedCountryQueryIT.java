package com.matchmap.api.geo;

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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class MergedCountryQueryIT {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

  @BeforeEach
  void mergeXaIntoXb() {
    jdbcTemplate.update("DELETE FROM user_city_pin");
    jdbcTemplate.update("DELETE FROM city");
    jdbcTemplate.update("DELETE FROM country_name");
    jdbcTemplate.update("DELETE FROM country");
    jdbcTemplate.update(
        "INSERT INTO country (id, code, name) VALUES"
            + " ('00000000-0000-0000-0000-000000000501', 'XB', 'Successor Land'),"
            + " ('00000000-0000-0000-0000-000000000502', 'XA', 'Merged Land')");
    jdbcTemplate.update(
        "UPDATE country SET successor_id = '00000000-0000-0000-0000-000000000501'"
            + " WHERE code = 'XA'");
    jdbcTemplate.update(
        "INSERT INTO city (id, city_key, country_id, name, latitude, longitude) VALUES"
            + " ('00000000-0000-0000-0000-000000000503', 'xb-moved-town',"
            + " '00000000-0000-0000-0000-000000000501', 'Moved Town', 10.000000, 20.000000)");
  }

  @Test
  @DisplayName("Merged country is returned as its successor")
  void mergedCountryIsNotReturnedAndTheSuccessorHoldsItsCities() throws Exception {
    var response =
        mockMvc
            .post()
            .uri("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(
                JSON.writeValueAsString(Map.of("query", "{ countries { code cities { name } } }")))
            .exchange();

    JsonNode countries =
        JSON.readTree(response.getResponse().getContentAsString()).at("/data/countries");
    List<String> codes = new ArrayList<>();
    List<String> successorCityNames = new ArrayList<>();
    countries.forEach(
        country -> {
          codes.add(country.at("/code").asString());
          if ("XB".equals(country.at("/code").asString())) {
            country
                .at("/cities")
                .forEach(city -> successorCityNames.add(city.at("/name").asString()));
          }
        });
    assertThat(codes).doesNotContain("XA").contains("XB");
    assertThat(successorCityNames).containsExactly("Moved Town");
  }
}
