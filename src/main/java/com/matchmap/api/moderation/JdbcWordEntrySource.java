package com.matchmap.api.moderation;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class JdbcWordEntrySource implements WordEntrySource {

  private static final String ACTIVE_WORDS =
      "select normalized, category, active from %s where active";

  private final JdbcTemplate jdbcTemplate;

  JdbcWordEntrySource(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public WordLists load() {
    return new WordLists(query("prohibited_word"), query("reserved_word"));
  }

  private List<WordEntry> query(String table) {
    return jdbcTemplate.query(
        ACTIVE_WORDS.formatted(table),
        (rs, rowNum) ->
            new WordEntry(
                rs.getString("normalized"), rs.getString("category"), rs.getBoolean("active")));
  }
}
