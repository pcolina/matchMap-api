package com.matchmap.api.geo;

import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CityCoordinatesIT {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

  @BeforeEach
  void loadMadrid() {
    jdbcTemplate.update("DELETE FROM city");
    jdbcTemplate.update("DELETE FROM country_name");
    jdbcTemplate.update("DELETE FROM country");
    jdbcTemplate.update(
        "INSERT INTO country (id, code, name) VALUES"
            + " ('00000000-0000-0000-0000-000000000401', 'ES', 'Spain')");
    jdbcTemplate.update(
        "INSERT INTO city (id, city_key, country_id, name, latitude, longitude) VALUES"
            + " ('00000000-0000-0000-0000-000000000402', 'es-madrid',"
            + " '00000000-0000-0000-0000-000000000401', 'Madrid', 40.416800, -3.703800)");
  }

  @Test
  void cityCoordinatesAreReturnedForTheCitiesOfACountry() throws Exception {
    var response = query("{ countries { code cities { name latitude longitude } } }");

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    var city =
        JSON.readTree(response.getResponse().getContentAsString()).at("/data/countries/0/cities/0");
    assertThat(city.at("/name").asString()).isEqualTo("Madrid");
    assertThat(city.at("/latitude").asDouble()).isEqualTo(40.4168);
    assertThat(city.at("/longitude").asDouble()).isEqualTo(-3.7038);
  }

  @Test
  void everyFieldOfCityResolves() {
    var response =
        query("{ countries { cities { id name latitude longitude country { code name } } } }");

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
  }

  private MvcTestResult query(String graphql) {
    return mockMvc
        .post()
        .uri("/graphql")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(JSON.writeValueAsString(Map.of("query", graphql)))
        .exchange();
  }
}
