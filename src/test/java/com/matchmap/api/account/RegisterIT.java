package com.matchmap.api.account;

import static com.matchmap.api.support.GraphqlRequests.REGISTER;
import static com.matchmap.api.support.GraphqlRequests.body;
import static com.matchmap.api.support.GraphqlRequests.registerBody;
import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import java.util.Map;
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
  void successfulRegistrationGeneratesUsernameFromEmail() {
    String suffix = uniqueSuffix();

    var response = graphql(registerBody("ana" + suffix + "@example.com", PASSWORD));

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
        .isEqualTo("ana" + suffix);
    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.register.createdAt")
        .asString()
        .isNotBlank();
  }

  @Test
  void responseNeverContainsPassword() throws Exception {
    String suffix = uniqueSuffix();

    var response = graphql(registerBody("bob" + suffix + "@example.com", PASSWORD));
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

    var response = graphql(registerBody("  Ana" + suffix + "@Example.COM ", PASSWORD));

    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.register.email")
        .isEqualTo("ana" + suffix + "@example.com");
  }

  @Test
  void rejectsTakenEmail() {
    String suffix = uniqueSuffix();
    graphql(registerBody("taken" + suffix + "@example.com", PASSWORD));

    var response = graphql(registerBody("TAKEN" + suffix + "@example.com", PASSWORD));

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
  }

  @Test
  void rejectsInvalidEmail() {
    var response = graphql(registerBody("not-an-email", PASSWORD));

    assertValidationFailed(response, "email");
  }

  @Test
  void rejectsTooShortPassword() {
    String suffix = uniqueSuffix();

    var response = registerPassword(suffix, "Ab1!x");

    assertPasswordInvalid(response);
  }

  @Test
  void rejectsTooLongPassword() {
    String suffix = uniqueSuffix();

    var response = registerPassword(suffix, "Abcdefghijklmnopqrs1!");

    assertPasswordInvalid(response);
  }

  @Test
  void acceptsPasswordsAtTheLimits() {
    String suffix = uniqueSuffix();

    var shortest = registerPassword("min" + suffix, "Abcd1!");
    var longest = registerPassword("max" + suffix, "Abcdefghijklmnopqr1!");

    assertThat(shortest)
        .bodyJson()
        .extractingPath("$.data.register.username")
        .isEqualTo("pwmin" + suffix);
    assertThat(longest)
        .bodyJson()
        .extractingPath("$.data.register.username")
        .isEqualTo("pwmax" + suffix);
  }

  @Test
  void rejectsPasswordWithoutLowercaseLetter() {
    String suffix = uniqueSuffix();

    var response = registerPassword(suffix, "ABCDEFGHIJK1!");

    assertPasswordInvalid(response);
  }

  @Test
  void rejectsPasswordWithoutUppercaseLetter() {
    String suffix = uniqueSuffix();

    var response = registerPassword(suffix, "abcdefghijk1!");

    assertPasswordInvalid(response);
  }

  @Test
  void rejectsPasswordWithoutSpecialCharacter() {
    String suffix = uniqueSuffix();

    var response = registerPassword(suffix, "Abcdefghijkl12");

    assertPasswordInvalid(response);
  }

  @Test
  void rejectsLettersAndDigitsAsSpecialCharacters() {
    String suffix = uniqueSuffix();

    var digitsOnly = registerPassword("digits" + suffix, "Abcdefghijkl12");
    var unicodeLetter = registerPassword("accent" + suffix, "Abcdefghijkl1é");

    assertPasswordInvalid(digitsOnly);
    assertPasswordInvalid(unicodeLetter);
  }

  @Test
  void acceptsEverySpecialCharacterFromTheOwaspList() {
    String suffix = uniqueSuffix();
    String specials = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

    for (int i = 0; i < specials.length(); i++) {
      var response = registerPassword("sp" + i + suffix, "Abcdefghijk1" + specials.charAt(i));

      assertThat(response).bodyJson().doesNotHavePath("$.errors");
    }
  }

  @Test
  void acceptsUnicodeCharacters() {
    String suffix = uniqueSuffix();

    var response = registerPassword(suffix, "Ñandú!Ab");

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
  }

  @Test
  void rejectsNonBreakingSpace() {
    String suffix = uniqueSuffix();

    var response = registerPassword(suffix, "Abcdefghijk 1!");

    assertPasswordInvalid(response);
  }

  @Test
  void rejectsPasswordWithWhitespaceInside() {
    String suffix = uniqueSuffix();

    var response = graphql(registerBody("space" + suffix + "@example.com", "secret pass12"));

    assertPasswordInvalid(response);
  }

  @Test
  void rejectsPasswordWithSurroundingWhitespace() {
    String suffix = uniqueSuffix();

    var leading = graphql(registerBody("lead" + suffix + "@example.com", " secret pass12"));
    var trailing = graphql(registerBody("trail" + suffix + "@example.com", "secret pass12 "));

    assertPasswordInvalid(leading);
    assertPasswordInvalid(trailing);
  }

  @Test
  void takenGeneratedUsernameGetsCountingSuffix() {
    String suffix = uniqueSuffix();
    graphql(registerBody("tk" + suffix + "@example.com", PASSWORD));

    var response = graphql(registerBody("tk" + suffix + "@other.example", PASSWORD));

    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.register.username")
        .isEqualTo("tk" + suffix + "_2");
  }

  @Test
  void shortEmailPrefixGetsRandomUsername() {
    String suffix = uniqueSuffix();

    var response = graphql(registerBody("a@short" + suffix + ".example", PASSWORD));

    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.register.username")
        .asString()
        .matches("user_[a-z0-9]{8}");
  }

  @Test
  void uppercaseAndDotsAreSanitizedInUsername() {
    String suffix = uniqueSuffix();

    var response = graphql(registerBody("Ana.Travels" + suffix + "@Example.com", PASSWORD));

    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.register.username")
        .isEqualTo("ana_travels" + suffix.toLowerCase());
  }

  @Test
  void longPrefixIsCutAt30Characters() {
    String suffix = uniqueSuffix();
    String prefix = "longprefix" + suffix + "abcdefghijklmnopqrstuvwxyz";

    var response = graphql(registerBody(prefix + "@example.com", PASSWORD));

    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.register.username")
        .isEqualTo(prefix.substring(0, 30));
  }

  @Test
  void inputWithRemovedFieldIsRejected() {
    String email = "removed" + uniqueSuffix() + "@example.com";

    var response =
        graphql(
            body(
                REGISTER,
                Map.of(
                    "input", Map.of("email", email, "password", PASSWORD, "displayName", "Ana"))));

    assertThat(response)
        .bodyJson()
        .extractingPath("$.errors[0].message")
        .asString()
        .contains("displayName");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_account WHERE email = ?", Integer.class, email))
        .isZero();
  }

  private MvcTestResult registerPassword(String suffix, String password) {
    return graphql(registerBody("pw" + suffix + "@example.com", password));
  }

  private void assertPasswordInvalid(MvcTestResult response) {
    assertThat(response)
        .bodyJson()
        .extractingPath("$.errors[0].extensions.code")
        .isEqualTo("PASSWORD_INVALID");
    assertThat(response)
        .bodyJson()
        .extractingPath("$.errors[0].extensions.field")
        .isEqualTo("password");
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
