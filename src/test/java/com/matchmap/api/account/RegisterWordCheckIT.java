package com.matchmap.api.account;

import static com.matchmap.api.support.GraphqlRequests.registerBody;
import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import java.time.OffsetDateTime;
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
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegisterWordCheckIT {

  private static final String PASSWORD = "Correcthorse1!";

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void prohibitedPrefixGetsAFallbackUsername() {
    insertWord("prohibited_word", "nazi", "nazi", "HATE");

    var response = graphql(registerBody("nazi.fan" + uniqueSuffix() + "@example.com", PASSWORD));

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.register.username")
        .asString()
        .matches("user_[a-z0-9]{8}");
  }

  @Test
  void reservedPrefixGetsAFallbackUsername() {
    insertWord("reserved_word", "support", "suport", "RESERVED");

    var response = graphql(registerBody("support" + uniqueSuffix() + "@example.com", PASSWORD));

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(response)
        .bodyJson()
        .extractingPath("$.data.register.username")
        .asString()
        .matches("user_[a-z0-9]{8}");
  }

  private void insertWord(String table, String word, String normalized, String category) {
    var now = OffsetDateTime.now();
    jdbcTemplate.update(
        "insert into "
            + table
            + " (id, word, normalized, category, language, active, created_at, updated_at)"
            + " values (?, ?, ?, ?, 'en', true, ?, ?)",
        UUID.randomUUID(),
        word,
        normalized,
        category,
        now,
        now);
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
