package com.matchmap.api.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ReservedWordSeedIT {

  private static final List<String> APPROVED_NAMES =
      List.of(
          "admin",
          "administrator",
          "administrador",
          "root",
          "superadmin",
          "support",
          "soporte",
          "staff",
          "moderator",
          "moderador",
          "matchmap",
          "system",
          "sistema");

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void seedsApprovedNames() {
    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList("select word, category, active from reserved_word");

    assertThat(rows)
        .extracting(row -> row.get("word"))
        .containsExactlyInAnyOrderElementsOf(APPROVED_NAMES);
    assertThat(rows)
        .allSatisfy(
            row -> {
              assertThat(row.get("category")).isEqualTo("RESERVED");
              assertThat(row.get("active")).isEqualTo(true);
            });
  }

  @Test
  void storesNormalizedForms() {
    assertThat(normalizedOf("support")).isEqualTo("suport");
    assertThat(normalizedOf("root")).isEqualTo("rot");
    assertThat(normalizedOf("staff")).isEqualTo("staf");
  }

  @Test
  void prohibitedTableIsEmptyAfterMigration() {
    assertThat(jdbcTemplate.queryForObject("select count(*) from prohibited_word", Integer.class))
        .isZero();
  }

  private String normalizedOf(String word) {
    return jdbcTemplate.queryForObject(
        "select normalized from reserved_word where word = ?", String.class, word);
  }
}
