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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RegisterIT {

  private static final String PASSWORD = "correct horse battery";

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

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

  @Test
  void normalizesEmail() {
    String suffix = uniqueSuffix();

    var response =
        graphql(
            registerBody("  Ana" + suffix + "@Example.COM ", "norm_" + suffix, "Ana", PASSWORD));

    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.register.email")
        .isEqualTo("ana" + suffix + "@example.com");
  }

  @Test
  void rejectsTakenEmail() {
    String suffix = uniqueSuffix();
    graphql(registerBody("taken" + suffix + "@example.com", "first_" + suffix, "Ana", PASSWORD));

    var response =
        graphql(
            registerBody("TAKEN" + suffix + "@example.com", "second_" + suffix, "Ana", PASSWORD));

    assertThat(response)
        .bodyJson()
        .extractingPath("$.errors[0].extensions.code")
        .isEqualTo("EMAIL_TAKEN");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_account WHERE email = ?",
                Integer.class,
                "taken" + suffix + "@example.com"))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_account WHERE username = ?",
                Integer.class,
                "second_" + suffix))
        .isZero();
  }

  @Test
  void rejectsTakenUsername() {
    String suffix = uniqueSuffix();
    graphql(registerBody("first" + suffix + "@example.com", "taken_" + suffix, "Ana", PASSWORD));

    var response =
        graphql(
            registerBody("second" + suffix + "@example.com", "TAKEN_" + suffix, "Ana", PASSWORD));

    assertThat(response)
        .bodyJson()
        .extractingPath("$.errors[0].extensions.code")
        .isEqualTo("USERNAME_TAKEN");
  }

  @Test
  void rejectsInvalidEmail() {
    String suffix = uniqueSuffix();

    var response = graphql(registerBody("not-an-email", "invalid_" + suffix, "Ana", PASSWORD));

    assertValidationFailed(response, "email");
  }

  @Test
  void rejectsInvalidUsername() {
    String suffix = uniqueSuffix();

    var response = graphql(registerBody("short" + suffix + "@example.com", "a", "Ana", PASSWORD));

    assertValidationFailed(response, "username");
  }

  @Test
  void rejectsBlankDisplayName() {
    String suffix = uniqueSuffix();

    var response =
        graphql(
            registerBody("blank" + suffix + "@example.com", "blank_" + suffix, "   ", PASSWORD));

    assertValidationFailed(response, "displayName");
  }

  @Test
  void rejectsTooShortPassword() {
    String suffix = uniqueSuffix();

    var response =
        graphql(
            registerBody(
                "short" + suffix + "@example.com", "short_" + suffix, "Ana", "a".repeat(11)));

    assertValidationFailed(response, "password");
  }

  @Test
  void rejectsTooLongPassword() {
    String suffix = uniqueSuffix();

    var response =
        graphql(
            registerBody(
                "long" + suffix + "@example.com", "long_" + suffix, "Ana", "a".repeat(129)));

    assertValidationFailed(response, "password");
  }

  private void assertValidationFailed(MvcTestResult response, String field) {
    assertThat(response)
        .bodyJson()
        .extractingPath("$.errors[0].extensions.code")
        .isEqualTo("VALIDATION_FAILED");
    assertThat(response).bodyJson().extractingPath("$.errors[0].extensions.field").isEqualTo(field);
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
