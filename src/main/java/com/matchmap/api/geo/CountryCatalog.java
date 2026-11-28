package com.matchmap.api.geo;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class CountryCatalog {

  private static final Set<String> NAME_LANGUAGES = Set.of("es", "de", "fr", "ja", "zh-Hant", "ko");

  private final JdbcTemplate jdbcTemplate;

  CountryCatalog(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  List<Country> findAll() {
    return jdbcTemplate.query(
        "SELECT c.id, c.code, COALESCE(n.name, c.name) AS name FROM country c"
            + " LEFT JOIN country_name n ON n.country_id = c.id AND n.language = ?"
            + " WHERE c.successor_id IS NULL ORDER BY c.code",
        (rs, rowNum) -> country(rs),
        requestedLanguage());
  }

  Country findByCode(String code) {
    return jdbcTemplate.queryForObject(
        "SELECT c.id, c.code, COALESCE(n.name, c.name) AS name FROM country c"
            + " LEFT JOIN country_name n ON n.country_id = c.id AND n.language = ?"
            + " WHERE c.code = ?",
        (rs, rowNum) -> country(rs),
        requestedLanguage(),
        code);
  }

  Country findById(UUID id) {
    return jdbcTemplate.queryForObject(
        "SELECT c.id, c.code, COALESCE(n.name, c.name) AS name FROM country c"
            + " LEFT JOIN country_name n ON n.country_id = c.id AND n.language = ?"
            + " WHERE c.id = ?",
        (rs, rowNum) -> country(rs),
        requestedLanguage(),
        id);
  }

  private static Country country(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new Country(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"));
  }

  private static String requestedLanguage() {
    String tag = LocaleContextHolder.getLocale().toLanguageTag();
    return NAME_LANGUAGES.contains(tag) ? tag : null;
  }
}
