package com.matchmap.api.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import com.matchmap.api.auth.RefreshToken;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
class AccountSchemaIT {

  @Autowired JdbcTemplate jdbcTemplate;

  @Autowired EntityManager entityManager;

  @Test
  void createsAccountAndRefreshTokenTables() {
    assertThat(columnsOf("user_account"))
        .containsExactlyInAnyOrder(
            "id",
            "email",
            "username",
            "password_hash",
            "google_subject",
            "created_at");
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
