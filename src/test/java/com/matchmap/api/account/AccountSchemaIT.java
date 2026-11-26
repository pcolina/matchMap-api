package com.matchmap.api.account;

import static com.matchmap.api.support.GraphqlRequests.body;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import com.matchmap.api.auth.RefreshToken;
import jakarta.persistence.EntityManager;
import java.util.List;
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
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
class AccountSchemaIT {

  @Autowired JdbcTemplate jdbcTemplate;

  @Autowired MockMvcTester mockMvc;

  @Test
  void firstVisitIsNotWritable() {
    var response =
        graphql(
            body(
                "mutation { register(input: { email: \"ana@example.com\", password:"
                    + " \"Correcthorse1!\", isFirstVisit: false }) { id } }"));

    assertThat(response)
        .bodyJson()
        .extractingPath("$.errors[0].extensions.classification")
        .isEqualTo("ValidationError");
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

  @Autowired EntityManager entityManager;

  @Test
  void createsAccountAndRefreshTokenTables() {
    assertThat(columnsOf("user_account"))
        .containsExactlyInAnyOrder(
            "id", "email", "username", "password_hash", "google_subject", "created_at", "first_visit");
    assertThat(columnsOf("refresh_token"))
        .containsExactlyInAnyOrder(
            "id",
            "account_id",
            "session_id",
            "token_hash",
            "created_at",
            "expires_at",
            "used_at",
            "revoked_at");
    assertThat(uniqueColumnsOf("user_account"))
        .containsExactlyInAnyOrder("email", "username", "google_subject");
    assertThat(uniqueColumnsOf("refresh_token")).containsExactly("token_hash");
    assertThat(entityManager.getMetamodel().entity(UserAccount.class)).isNotNull();
    assertThat(entityManager.getMetamodel().entity(RefreshToken.class)).isNotNull();
  }

  private List<String> columnsOf(String table) {
    return jdbcTemplate.queryForList(
        "SELECT column_name FROM information_schema.columns WHERE table_name = ?",
        String.class,
        table);
  }

  private List<String> uniqueColumnsOf(String table) {
    return jdbcTemplate.queryForList(
        """
        SELECT kcu.column_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name
        WHERE tc.table_name = ? AND tc.constraint_type = 'UNIQUE'
        """,
        String.class,
        table);
  }
}
