package com.matchmap.api.account;

import static com.matchmap.api.support.GraphqlRequests.body;
import static com.matchmap.api.support.GraphqlRequests.registerBody;
import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import java.util.UUID;
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
class RegisterIT {

  private static final String PASSWORD = "correct horse battery";

  @Autowired MockMvcTester mockMvc;

  @Test
  void registersAccount() {
    String suffix = uniqueSuffix();

    var response =
        graphql(registerBody("ana" + suffix + "@example.com", "ana_" + suffix, "Ana", PASSWORD));

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.register.id")
        .asString()
        .satisfies(id -> UUID.fromString(id));
    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.register.email")
        .isEqualTo("ana" + suffix + "@example.com");
    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.register.username")
        .isEqualTo("ana_" + suffix);
    assertThat(response).bodyJson().extractingPath("$.data.register.displayName").isEqualTo("Ana");
    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.register.createdAt")
        .asString()
        .isNotBlank();
  }

  @Test
  void responseNeverContainsPassword() throws Exception {
    String suffix = uniqueSuffix();

    var response =
        graphql(registerBody("bob" + suffix + "@example.com", "bob_" + suffix, "Bob", PASSWORD));
    var userType = graphql(body("{ __type(name: \"User\") { fields { name } } }"));

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(response.getResponse().getContentAsString()).doesNotContain(PASSWORD);
    assertThat(userType)
        .bodyJson()
        .extractingPath("$.data.__type.fields[*].name")
        .asArray()
        .isNotEmpty()
        .noneSatisfy(name -> assertThat(name.toString().toLowerCase()).contains("password"));
  }

  private MvcTestResult graphql(String requestBody) {
    return mockMvc
        .post()
        .uri("/graphql")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(requestBody)
        .exchange();
  }
}
