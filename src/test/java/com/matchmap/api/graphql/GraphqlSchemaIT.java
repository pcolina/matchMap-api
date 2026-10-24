package com.matchmap.api.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class GraphqlSchemaIT {

  @Autowired MockMvcTester mockMvc;

  @Test
  void rejectsUnknownField() {
    var response =
        mockMvc
            .post()
            .uri("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content("{\"query\": \"{ unknownField }\"}")
            .exchange();

    assertThat(response)
        .bodyJson()
        .extractingPath("$.errors[0].extensions.classification")
        .isEqualTo("ValidationError");
    assertThat(response).bodyJson().doesNotHavePath("$.data.unknownField");
  }
}
