package com.matchmap.api.moderation;

import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ProhibitedWordAdminIT {

  private static final String PASSWORD = "Correcthorse1!";
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String CREATE =
      "mutation($word: String!, $category: ProhibitedCategory!, $language: String!)"
          + " { createProhibitedWord(word: $word, category: $category, language: $language)"
          + " { id word normalized active } }";
  private static final String LIST = "{ prohibitedWords { id word category active } }";
  private static final String SET_ACTIVE =
      "mutation($id: ID!, $active: Boolean!) { setProhibitedWordActive(id: $id, active: $active)"
          + " { id active } }";
  private static final String BLOCKED_USERNAME = "xexamplex";

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

  @Autowired UsernameModeration usernameModeration;

  @Autowired WordCache wordCache;

  @BeforeEach
  void emptyList() {
    jdbcTemplate.update("DELETE FROM prohibited_word");
    wordCache.reload();
  }

  @AfterEach
  void restoreList() {
    jdbcTemplate.update("DELETE FROM prohibited_word");
    wordCache.reload();
  }

  @Test
  @DisplayName("Administrator creates a prohibited word")
  void administratorCreatesAProhibitedWord() throws Exception {
    String admin = signedInAccount(true);

    JsonNode created =
        call(CREATE, Map.of("word", "Example", "category", "HATE", "language", "en"), admin);

    assertThat(created.at("/data/createProhibitedWord/word").asString()).isEqualTo("Example");
    assertThat(created.at("/data/createProhibitedWord/normalized").asString()).isEqualTo("example");
    assertThat(created.at("/data/createProhibitedWord/active").asBoolean()).isTrue();
  }

  @Test
  @DisplayName("Duplicate word is rejected")
  void duplicateWordIsRejected() throws Exception {
    String admin = signedInAccount(true);
    call(CREATE, Map.of("word", "Example", "category", "HATE", "language", "en"), admin);

    JsonNode duplicate =
        call(CREATE, Map.of("word", "E-x-a-m-p-l-e", "category", "HATE", "language", "en"), admin);

    assertThat(duplicate.at("/errors/0/extensions/code").asString()).isEqualTo("DUPLICATE_WORD");
    assertThat(duplicate.at("/errors/0/message").asString())
        .doesNotContain("E-x-a-m-p-l-e")
        .doesNotContain("Example");
    assertThat(count()).isEqualTo(1);
  }

  @Test
  @DisplayName("Invalid word is rejected")
  void invalidWordIsRejected() throws Exception {
    String admin = signedInAccount(true);

    JsonNode blank =
        call(CREATE, Map.of("word", "   ", "category", "HATE", "language", "en"), admin);
    JsonNode tooLong =
        call(CREATE, Map.of("word", "x".repeat(101), "category", "HATE", "language", "en"), admin);

    assertThat(blank.at("/errors/0/extensions/code").asString()).isEqualTo("VALIDATION_FAILED");
    assertThat(blank.at("/errors/0/extensions/field").asString()).isEqualTo("word");
    assertThat(tooLong.at("/errors/0/extensions/field").asString()).isEqualTo("word");
    assertThat(count()).isZero();
  }

  @Test
  @DisplayName("New word applies at once")
  void newWordAppliesAtOnce() throws Exception {
    String admin = signedInAccount(true);

    call(CREATE, Map.of("word", "Example", "category", "HATE", "language", "en"), admin);

    assertThat(usernameModeration.isBlocked(BLOCKED_USERNAME)).isTrue();
  }

  @Test
  @DisplayName("Administrator lists the prohibited words")
  void administratorListsTheProhibitedWords() throws Exception {
    String admin = signedInAccount(true);
    call(CREATE, Map.of("word", "Example", "category", "HATE", "language", "en"), admin);
    JsonNode other =
        call(CREATE, Map.of("word", "Other", "category", "VIOLENCE", "language", "en"), admin);
    String otherId = other.at("/data/createProhibitedWord/id").asString();
    call(SET_ACTIVE, Map.of("id", otherId, "active", false), admin);

    JsonNode list = call(LIST, Map.of(), admin);

    assertThat(list.at("/data/prohibitedWords").size()).isEqualTo(2);
  }

  @Test
  @DisplayName("Administrator deactivates a word")
  void administratorDeactivatesAWord() throws Exception {
    String admin = signedInAccount(true);
    JsonNode created =
        call(CREATE, Map.of("word", "Example", "category", "HATE", "language", "en"), admin);
    String id = created.at("/data/createProhibitedWord/id").asString();

    JsonNode changed = call(SET_ACTIVE, Map.of("id", id, "active", false), admin);

    assertThat(changed.at("/data/setProhibitedWordActive/active").asBoolean()).isFalse();
    assertThat(count()).isEqualTo(1);
    assertThat(usernameModeration.isBlocked(BLOCKED_USERNAME)).isFalse();
  }

  @Test
  @DisplayName("Unknown word id is rejected")
  void unknownWordIdIsRejected() throws Exception {
    String admin = signedInAccount(true);

    JsonNode changed =
        call(SET_ACTIVE, Map.of("id", UUID.randomUUID().toString(), "active", false), admin);

    assertThat(changed.at("/errors/0/extensions/code").asString()).isEqualTo("VALIDATION_FAILED");
    assertThat(changed.at("/errors/0/extensions/field").asString()).isEqualTo("id");
  }

  @Test
  @DisplayName("Only administrators manage the list")
  void onlyAdministratorsManageTheList() throws Exception {
    String user = signedInAccount(false);

    JsonNode create =
        call(CREATE, Map.of("word", "Example", "category", "HATE", "language", "en"), user);
    JsonNode list = call(LIST, Map.of(), user);
    JsonNode change =
        call(SET_ACTIVE, Map.of("id", UUID.randomUUID().toString(), "active", false), user);

    assertThat(create.at("/errors/0/extensions/classification").asString()).isEqualTo("FORBIDDEN");
    assertThat(list.at("/errors/0/extensions/classification").asString()).isEqualTo("FORBIDDEN");
    assertThat(change.at("/errors/0/extensions/classification").asString()).isEqualTo("FORBIDDEN");
    assertThat(count()).isZero();
  }

  private long count() {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM prohibited_word", Long.class);
  }

  private String signedInAccount(boolean admin) throws Exception {
    String email = (admin ? "wordadmin" : "worduser") + uniqueSuffix() + "@example.com";
    call(
        "mutation { register(input: { email: \""
            + email
            + "\", password: \""
            + PASSWORD
            + "\" }) { id } }",
        Map.of(),
        null);
    if (admin) {
      jdbcTemplate.update("UPDATE user_account SET role = 'ADMIN' WHERE email = ?", email);
    }
    var login =
        mockMvc
            .post()
            .uri("/api/auth/login")
            .header("X-Client", "mobile")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(JSON.writeValueAsString(Map.of("email", email, "password", PASSWORD)))
            .exchange();
    return JSON.readTree(login.getResponse().getContentAsString()).get("accessToken").asString();
  }

  private JsonNode call(String query, Map<String, Object> variables, String token)
      throws Exception {
    Map<String, Object> body =
        variables.isEmpty()
            ? Map.of("query", query)
            : Map.of("query", query, "variables", variables);
    var request =
        mockMvc
            .post()
            .uri("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON);
    if (token != null) {
      request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
    MvcTestResult response = request.content(JSON.writeValueAsString(body)).exchange();
    return JSON.readTree(response.getResponse().getContentAsString());
  }
}
