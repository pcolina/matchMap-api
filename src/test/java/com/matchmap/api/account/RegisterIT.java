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

  private static final String PASSWORD = "Correcthorse1!";

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

    var response = registerPassword(suffix, "Ab1!x");

    assertValidationFailed(response, "password");
  }

  @Test
  void rejectsTooLongPassword() {
    String suffix = uniqueSuffix();

    var response = registerPassword(suffix, "Abcdefghijklmnopqrs1!");

    assertValidationFailed(response, "password");
  }

  @Test
  void acceptsPasswordsAtTheLimits() {
    String suffix = uniqueSuffix();

    var shortest = registerPassword("min" + suffix, "Abcd1!");
    var longest = registerPassword("max" + suffix, "Abcdefghijklmnopqr1!");

    assertThat(shortest)
        .bodyJson()
        .extractingPath("$.data.register.username")
        .isEqualTo("min" + suffix);
    assertThat(longest)
        .bodyJson()
        .extractingPath("$.data.register.username")
        .isEqualTo("max" + suffix);
  }

  @Test
  void rejectsPasswordWithoutLowercaseLetter() {
    String suffix = uniqueSuffix();

    var response = registerPassword(suffix, "ABCDEFGHIJK1!");

    assertValidationFailed(response, "password");
  }

  @Test
  void rejectsPasswordWithoutUppercaseLetter() {
    String suffix = uniqueSuffix();

    var response = registerPassword(suffix, "abcdefghijk1!");

    assertValidationFailed(response, "password");
  }

  @Test
  void rejectsPasswordWithoutSpecialCharacter() {
    String suffix = uniqueSuffix();

    var response = registerPassword(suffix, "Abcdefghijkl12");

    assertValidationFailed(response, "password");
  }

  @Test
  void rejectsLettersAndDigitsAsSpecialCharacters() {
    String suffix = uniqueSuffix();

    var digitsOnly = registerPassword("digits" + suffix, "Abcdefghijkl12");
    var unicodeLetter = registerPassword("accent" + suffix, "Abcdefghijkl1\u00e9");

    assertValidationFailed(digitsOnly, "password");
    assertValidationFailed(unicodeLetter, "password");
  }

  @Test
  void acceptsEverySpecialCharacterFromTheOwaspList() {
    String suffix = uniqueSuffix();
    String specials = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

    for (int i = 0; i < specials.length(); i++) {
      var response =
          registerPassword("sp" + i + suffix, "Abcdefghijk1" + specials.charAt(i));

      assertThat(response).bodyJson().doesNotHavePath("$.errors");
    }
  }

  @Test
  void acceptsUnicodeCharacters() {
    String suffix = uniqueSuffix();

    var response = registerPassword(suffix, "\u00d1and\u00fa!Ab");

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
  }

  @Test
  void rejectsNonBreakingSpace() {
    String suffix = uniqueSuffix();

    var response = registerPassword(suffix, "Abcdefghijk\u00a01!");

    assertValidationFailed(response, "password");
  }

  private MvcTestResult registerPassword(String suffix, String password) {
    return graphql(registerBody("pw" + suffix + "@example.com", "pw_" + suffix, "Ana", password));
  }

  @Test
  void rejectsPasswordWithWhitespaceInside() {
    String suffix = uniqueSuffix();

    var response =
        graphql(
            registerBody(
                "space" + suffix + "@example.com", "space_" + suffix, "Ana", "secret pass12"));

    assertValidationFailed(response, "password");
  }

  @Test
  void rejectsPasswordWithSurroundingWhitespace() {
    String suffix = uniqueSuffix();

    var leading =
        graphql(
            registerBody(
                "lead" + suffix + "@example.com", "lead_" + suffix, "Ana", " secret pass12"));
    var trailing =
        graphql(
            registerBody(
                "trail" + suffix + "@example.com", "trail_" + suffix, "Ana", "secret pass12 "));

    assertValidationFailed(leading, "password");
    assertValidationFailed(trailing, "password");
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
