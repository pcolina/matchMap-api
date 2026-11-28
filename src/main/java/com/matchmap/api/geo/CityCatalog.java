package com.matchmap.api.geo;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class CityCatalog {

  private final JdbcTemplate jdbcTemplate;

  CityCatalog(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  List<City> findActiveByCountryCode(String countryCode) {
    return jdbcTemplate.query(
        "SELECT city.id, city.name, city.latitude, city.longitude, country.code"
            + " FROM city JOIN country ON country.id = city.country_id"
            + " WHERE country.code = ? AND city.active ORDER BY city.name",
        (rs, rowNum) ->
            new City(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"),
                rs.getString("code")),
        countryCode);
  }
}
