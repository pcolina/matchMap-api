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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CountryNameLanguageIT {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String COUNTRIES = "{ countries { code name } }";

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

  @BeforeEach
  void loadFrance() {
    jdbcTemplate.update("DELETE FROM user_city_pin");
    jdbcTemplate.update("DELETE FROM city");
    jdbcTemplate.update("DELETE FROM country_name");
    jdbcTemplate.update("DELETE FROM country");
    jdbcTemplate.update(
        "INSERT INTO country (id, code, name) VALUES"
            + " ('00000000-0000-0000-0000-000000000601', 'FR', 'France')");
    jdbcTemplate.update(
        "INSERT INTO country_name (country_id, language, name) VALUES"
            + " ('00000000-0000-0000-0000-000000000601', 'es', 'Francia'),"
            + " ('00000000-0000-0000-0000-000000000601', 'ja', 'ãƒ•ãƒ©ãƒ³ã‚¹'),"
            + " ('00000000-0000-0000-0000-000000000601', 'zh-Hant', 'æ³•åœ‹')");
  }

  @Test
  @DisplayName("Country name in the requested language")
  void spanishNameIsReturnedForSpanish() throws Exception {
    assertThat(franceName("es")).isEqualTo("Francia");
  }

  @Test
  @DisplayName("Fallback to the source language")
  void sourceNameIsReturnedWhenTheLanguageHasNoName() throws Exception {
    assertThat(franceName("de")).isEqualTo("France");
  }

  @Test
  @DisplayName("Japanese and Traditional Chinese names")
  void japaneseAndTraditionalChineseNamesAreReturned() throws Exception {
    assertThat(franceName("ja")).isEqualTo("ãƒ•ãƒ©ãƒ³ã‚¹");
    assertThat(franceName("zh-Hant")).isEqualTo("æ³•åœ‹");
  }

  private String franceName(String acceptLanguage) throws Exception {
    var response =
        mockMvc
            .post()
            .uri("/graphql")
            .header(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(JSON.writeValueAsString(Map.of("query", COUNTRIES)))
            .exchange();
    List<String> names = new ArrayList<>();
    JsonNode countries =
        JSON.readTree(response.getResponse().getContentAsString()).at("/data/countries");
    countries.forEach(
        country -> {
          if ("FR".equals(country.at("/code").asString())) {
            names.add(country.at("/name").asString());
          }
        });
    return names.get(0);
  }
}
