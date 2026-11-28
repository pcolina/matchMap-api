package com.matchmap.api.geo;

import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import org.assertj.core.api.InstanceOfAssertFactories;
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
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CountriesQueryIT {

  @Autowired MockMvcTester mockMvc;
  @Autowired JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearCatalog() {
    jdbcTemplate.update("DELETE FROM user_city_pin");
    jdbcTemplate.update("DELETE FROM city");
    jdbcTemplate.update("DELETE FROM country_name");
    jdbcTemplate.update("DELETE FROM country");
  }

  @Test
  @DisplayName("Catalog data is read from the database; Requests do not read the data set")
  void countriesAreReadFromTheDatabase() {
    jdbcTemplate.update(
        "INSERT INTO country (id, code, name) VALUES (?, 'FR', 'France')",
        java.util.UUID.fromString("00000000-0000-0000-0000-000000000301"));

    var response = query("{ countries { code name } }");

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.countries[?(@.code=='FR')].name")
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .containsExactly("France");
  }

  private MvcTestResult query(String graphql) {
    return mockMvc
        .post()
        .uri("/graphql")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content("{\"query\": \"" + graphql.replace("\"", "\\\"") + "\"}")
        .exchange();
  }
}
