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
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class WordCacheIT {

  @Autowired WordCache wordCache;

  @Autowired JdbcWordEntrySource source;

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void loadsActiveWordsAtStartup() {
    insertWord("prohibited_word", "nazism", "HATE", true);
    insertWord("prohibited_word", "bannedword", "HATE", false);

    var cache = new WordCache(source);

    assertThat(cache.current().isBlocked("nazism_fan")).isTrue();
    assertThat(cache.current().isBlocked("bannedword_fan")).isFalse();
  }

  @Test
  void reloadPicksUpNewWord() {
    assertThat(wordCache.current().isBlocked("nazism_fan")).isFalse();

    insertWord("prohibited_word", "nazism", "HATE", true);
    wordCache.reload();

    assertThat(wordCache.current().isBlocked("nazism_fan")).isTrue();
  }

  @AfterTransaction
  void reloadAfterRollback() {
    wordCache.reload();
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
