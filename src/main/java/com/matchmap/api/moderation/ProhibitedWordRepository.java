package com.matchmap.api.moderation;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class ProhibitedWordRepository {

  private static final String COLUMNS = "id, word, normalized, category, language, active";

  private final JdbcTemplate jdbcTemplate;

  ProhibitedWordRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  boolean exists(String normalized, String category) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM prohibited_word WHERE normalized = ? AND category = ?",
            Integer.class,
            normalized,
            category);
    return count != 0;
  }

  UUID insert(String word, String normalized, String category, String language) {
    return jdbcTemplate.queryForObject(
        "INSERT INTO prohibited_word (id, word, normalized, category, language, active,"
            + " created_at, updated_at) VALUES (gen_random_uuid(), ?, ?, ?, ?, true, now(), now())"
            + " RETURNING id",
        UUID.class,
        word,
        normalized,
        category,
        language);
  }

  boolean exists(UUID id) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM prohibited_word WHERE id = ?", Integer.class, id);
    return count != 0;
  }

  void setActive(UUID id, boolean active) {
    jdbcTemplate.update(
        "UPDATE prohibited_word SET active = ?, updated_at = now() WHERE id = ?", active, id);
  }

  ProhibitedWord find(UUID id) {
    return jdbcTemplate.queryForObject(
        "SELECT " + COLUMNS + " FROM prohibited_word WHERE id = ?", (rs, rowNum) -> row(rs), id);
  }

  List<ProhibitedWord> findAll() {
    return jdbcTemplate.query(
        "SELECT " + COLUMNS + " FROM prohibited_word ORDER BY category, word",
        (rs, rowNum) -> row(rs));
  }

  private static ProhibitedWord row(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new ProhibitedWord(
        rs.getObject("id", UUID.class),
        rs.getString("word"),
        rs.getString("normalized"),
        rs.getString("category"),
        rs.getString("language"),
        rs.getBoolean("active"));
  }
}
