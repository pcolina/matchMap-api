package com.matchmap.api.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.matchmap.api.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ProhibitedWordSchemaIT {

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void createsEmptyWordTables() {
    assertThat(countRows("prohibited_word")).isZero();
    assertThat(countRows("reserved_word")).isZero();
  }

  @Test
  void rejectsUnknownCategory() {
    assertThatThrownBy(() -> insert("prohibited_word", "nazi", "nazi", "UNKNOWN"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsDuplicateNormalizedFormInSameCategory() {
    insert("prohibited_word", "nazi", "nazi", "HATE");

    assertThatThrownBy(() -> insert("prohibited_word", "naz1", "nazi", "HATE"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void allowsSameNormalizedFormInAnotherCategory() {
    insert("reserved_word", "root", "root", "RESERVED");
    insert("reserved_word", "root", "root", "IMPERSONATION");

    assertThat(countRows("reserved_word")).isEqualTo(2);
  }

  private int countRows(String table) {
    return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
  }

  private void insert(String table, String word, String normalized, String category) {
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
}
