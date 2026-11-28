package com.matchmap.api.geo;

import java.util.List;
import java.util.Set;
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
        "SELECT c.code, COALESCE(n.name, c.name) AS name FROM country c"
            + " LEFT JOIN country_name n ON n.country_id = c.id AND n.language = ?"
            + " WHERE c.successor_id IS NULL ORDER BY c.code",
        (rs, rowNum) -> new Country(rs.getString("code"), rs.getString("name")),
        requestedLanguage());
  }

  Country findByCode(String code) {
    return jdbcTemplate.queryForObject(
        "SELECT c.code, COALESCE(n.name, c.name) AS name FROM country c"
            + " LEFT JOIN country_name n ON n.country_id = c.id AND n.language = ?"
            + " WHERE c.code = ?",
        (rs, rowNum) -> new Country(rs.getString("code"), rs.getString("name")),
        requestedLanguage(),
        code);
  }

  private static String requestedLanguage() {
    String tag = LocaleContextHolder.getLocale().toLanguageTag();
    return NAME_LANGUAGES.contains(tag) ? tag : null;
  }
}
