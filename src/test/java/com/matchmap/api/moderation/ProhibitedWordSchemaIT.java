package com.matchmap.api.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ProhibitedWordSchemaIT {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvcTester mockMvc;

  @Test
  @DisplayName("Schema exposes the prohibited word type, its operations and the category enum")
  void schemaExposesProhibitedWordOperations() throws Exception {
    JsonNode query = schema("{ __type(name: \"Query\") { fields { name } } }");
    JsonNode mutation = schema("{ __type(name: \"Mutation\") { fields { name } } }");
    JsonNode type = schema("{ __type(name: \"ProhibitedWord\") { fields { name } } }");
    JsonNode category = schema("{ __type(name: \"ProhibitedCategory\") { enumValues { name } } }");

    List<String> queries = names(query.at("/data/__type/fields"));
    List<String> mutations = names(mutation.at("/data/__type/fields"));
    List<String> fields = names(type.at("/data/__type/fields"));
    List<String> categories = names(category.at("/data/__type/enumValues"));

    assertThat(queries).contains("prohibitedWords");
    assertThat(mutations).contains("createProhibitedWord", "setProhibitedWordActive");
    assertThat(fields).contains("id", "word", "normalized", "category", "language", "active");
    assertThat(categories)
        .containsExactlyInAnyOrder(
            "CHILD_SAFETY", "HATE", "VIOLENCE", "SEXUAL", "IMPERSONATION", "RESERVED");
  }

  private JsonNode schema(String query) throws Exception {
    var response =
        mockMvc
            .post()
            .uri("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(JSON.writeValueAsString(java.util.Map.of("query", query)))
            .exchange();
    return JSON.readTree(response.getResponse().getContentAsString());
  }

  private static List<String> names(JsonNode nodes) {
    List<String> names = new ArrayList<>();
    nodes.forEach(node -> names.add(node.at("/name").asString()));
    return names;
  }
}
