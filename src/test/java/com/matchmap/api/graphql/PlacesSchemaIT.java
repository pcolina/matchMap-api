package com.matchmap.api.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PlacesSchemaIT {

  @Autowired MockMvcTester mockMvc;

  @Test
  void cityExposesCoordinates() {
    var response = query("{ __type(name: \"City\") { fields { name } } }");

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.__type.fields[*].name")
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .contains("id", "name", "latitude", "longitude", "country");
  }

  @Test
  void countryExposesDerivedStatusAndCities() {
    var response = query("{ __type(name: \"Country\") { fields { name } } }");

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.__type.fields[*].name")
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .contains("code", "name", "status", "cities");
  }

  @Test
  void pinStatusHasTheThreeAllowedValues() {
    var response = query("{ __type(name: \"PinStatus\") { enumValues { name } } }");

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.__type.enumValues[*].name")
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .containsExactlyInAnyOrder("VISITED", "WANT_TO_GO", "LIVED");
  }

  @Test
  void cityPinTypeIsDefined() {
    var response = query("{ __type(name: \"CityPin\") { fields { name } } }");

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.__type.fields[*].name")
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .contains("city", "status");
  }

  @Test
  void queryTypeExposesPlaceQueries() {
    var response = query("{ __type(name: \"Query\") { fields { name } } }");

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.__type.fields[*].name")
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .contains("countries", "cityPins");
  }

  @Test
  void mutationTypeExposesPinMutations() {
    var response = query("{ __type(name: \"Mutation\") { fields { name } } }");

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.__type.fields[*].name")
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .contains("setCityPin", "removeCityPin");
  }

  @Test
  void setCityPinTakesCityIdAndStatus() {
    var response = query("{ __type(name: \"Mutation\") { fields { name args { name } } } }");

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.__type.fields[?(@.name=='setCityPin')].args[*].name")
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .containsExactlyInAnyOrder("cityId", "status");
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
