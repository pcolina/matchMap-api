package com.matchmap.api.account;

import static com.matchmap.api.support.GraphqlRequests.body;
import static com.matchmap.api.support.GraphqlRequests.registerBody;
import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import java.util.Map;
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
class CompleteFirstVisitIT {

  private static final String PASSWORD = "Correcthorse1!";
  private static final String COMPLETE = "mutation { completeFirstVisit { id isFirstVisit } }";
  private static final String COMPLETE_ISFIRST = "mutation { completeFirstVisit { isFirstVisit } }";
  private static final String COMPLETE_WITH_ARGUMENT =
      "mutation { completeFirstVisit(isFirstVisit: true) { isFirstVisit } }";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void signedInUserCompletesFirstVisit() throws Exception {
    String email = "cora" + uniqueSuffix() + "@example.com";
    registerAccount(email);
    String token = login(email);

    var response = graphql(body(COMPLETE), token);

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.completeFirstVisit.isFirstVisit")
        .isEqualTo(false);
    assertThat(firstVisitOf(email)).isFalse();
  }

  @Test
  void completingAgainChangesNothing() throws Exception {
    String email = "dara" + uniqueSuffix() + "@example.com";
    registerAccount(email);
    String token = login(email);
    graphql(body(COMPLETE_ISFIRST), token);

    var response = graphql(body(COMPLETE_ISFIRST), token);

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.completeFirstVisit.isFirstVisit")
        .isEqualTo(false);
    assertThat(firstVisitOf(email)).isFalse();
  }

  @Test
  void anonymousClientCannotComplete() throws Exception {
    String email = "eli" + uniqueSuffix() + "@example.com";
    registerAccount(email);

    var response = graphql(body(COMPLETE_ISFIRST), null);

    assertThat(response)
        .bodyJson()
        .extractingPath("$.errors[0].extensions.classification")
        .isEqualTo("UNAUTHORIZED");
    assertThat(firstVisitOf(email)).isTrue();
  }

  @Test
  void completionAffectsOnlySignedInAccount() throws Exception {
    String suffix = uniqueSuffix();
    String first = "fia" + suffix + "@example.com";
    String second = "gus" + suffix + "@example.com";
    registerAccount(first);
    registerAccount(second);

    graphql(body(COMPLETE_ISFIRST), login(first));

    assertThat(firstVisitOf(first)).isFalse();
    assertThat(firstVisitOf(second)).isTrue();
  }

  @Test
  void mutationTakesNoArguments() throws Exception {
    String email = "hana" + uniqueSuffix() + "@example.com";
    registerAccount(email);
    String token = login(email);

    var withoutArgument = graphql(body(COMPLETE_ISFIRST), token);
    assertThat(withoutArgument)
        .bodyJson()
        .extractingPath("$.data.completeFirstVisit.isFirstVisit")
        .isEqualTo(false);

    var withArgument = graphql(body(COMPLETE_WITH_ARGUMENT), token);
    assertThat(withArgument)
        .bodyJson()
        .extractingPath("$.errors[0].extensions.classification")
        .isEqualTo("ValidationError");
  }

  private boolean firstVisitOf(String email) {
    return jdbcTemplate.queryForObject(
        "SELECT first_visit FROM user_account WHERE email = ?", Boolean.class, email);
  }

  private void registerAccount(String email) {
    graphql(registerBody(email, PASSWORD), null);
  }

  private String login(String email) throws Exception {
    var response =
        mockMvc
            .post()
            .uri("/api/auth/login")
            .header("X-Client", "mobile")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(JSON.writeValueAsString(Map.of("email", email, "password", PASSWORD)))
            .exchange();
    JsonNode body = JSON.readTree(response.getResponse().getContentAsString());
    return body.get("accessToken").asString();
  }

  private MvcTestResult graphql(String requestBody, String accessToken) {
    var request =
        mockMvc
            .post()
            .uri("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON);
    if (accessToken != null) {
      request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }
    return request.content(requestBody).exchange();
  }
}
