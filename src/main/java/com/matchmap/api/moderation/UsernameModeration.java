package com.matchmap.api.moderation;

import java.util.List;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UsernameModeration {

  private static final String ACTIVE_WORDS =
      "select normalized, category, active from %s where active";

  private final JdbcTemplate jdbcTemplate;

  public UsernameModeration(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public boolean isBlocked(String username) {
    return new ProhibitedWordMatcher(activeWords()).match(username).isPresent();
  }

  private List<WordEntry> activeWords() {
    var prohibited = query("prohibited_word");
    var reserved = query("reserved_word");
    return Stream.concat(prohibited.stream(), reserved.stream()).toList();
  }

  private List<WordEntry> query(String table) {
    return jdbcTemplate.query(
        ACTIVE_WORDS.formatted(table),
        (rs, rowNum) ->
            new WordEntry(
                rs.getString("normalized"), rs.getString("category"), rs.getBoolean("active")));
  }
}
