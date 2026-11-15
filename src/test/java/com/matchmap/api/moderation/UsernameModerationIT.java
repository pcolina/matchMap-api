package com.matchmap.api.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class UsernameModerationIT {

  @Autowired UsernameModeration usernameModeration;

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void blocksUsernameWithActiveProhibitedWord() {
    insertWord("prohibited_word", "nazi", "HATE", true);

    assertThat(usernameModeration.isBlocked("nazi_fan")).isTrue();
  }

  @Test
  void allowsUsernameWhenNoWordIsStored() {
    assertThat(usernameModeration.isBlocked("nazi_fan")).isFalse();
  }

  private void insertWord(String table, String word, String category, boolean active) {
    var now = OffsetDateTime.now();
    jdbcTemplate.update(
        "insert into "
            + table
            + " (id, word, normalized, category, language, active, created_at, updated_at)"
            + " values (?, ?, ?, ?, 'en', ?, ?, ?)",
        UUID.randomUUID(),
        word,
        word,
        category,
        active,
        now,
        now);
  }
}
