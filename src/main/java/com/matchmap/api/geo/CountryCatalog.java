package com.matchmap.api.geo;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class CountryCatalog {

  private final JdbcTemplate jdbcTemplate;

  CountryCatalog(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  List<Country> findAll() {
    return jdbcTemplate.query(
        "SELECT code, name FROM country ORDER BY code",
        (rs, rowNum) -> new Country(rs.getString("code"), rs.getString("name")));
  }

  Country findByCode(String code) {
    return jdbcTemplate.queryForObject(
        "SELECT code, name FROM country WHERE code = ?",
        (rs, rowNum) -> new Country(rs.getString("code"), rs.getString("name")),
        code);
  }
}
