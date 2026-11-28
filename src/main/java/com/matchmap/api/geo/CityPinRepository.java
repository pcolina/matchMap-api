package com.matchmap.api.geo;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class CityPinRepository {

  private final JdbcTemplate jdbcTemplate;

  CityPinRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  Boolean cityActive(UUID cityId) {
    List<Boolean> rows =
        jdbcTemplate.query(
            "SELECT active FROM city WHERE id = ?",
            (rs, rowNum) -> rs.getBoolean("active"),
            cityId);
    return rows.isEmpty() ? null : rows.get(0);
  }

  City findCity(UUID cityId) {
    return jdbcTemplate.queryForObject(
        "SELECT c.id, c.name, c.latitude, c.longitude, co.code FROM city c"
            + " JOIN country co ON co.id = c.country_id WHERE c.id = ?",
        (rs, rowNum) ->
            new City(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"),
                rs.getString("code")),
        cityId);
  }

  void upsert(UUID userId, UUID cityId, PinStatus status) {
    jdbcTemplate.update(
        "INSERT INTO user_city_pin (user_id, city_id, status, created_at, updated_at)"
            + " VALUES (?, ?, ?, now(), now())"
            + " ON CONFLICT (user_id, city_id) DO UPDATE SET status = EXCLUDED.status,"
            + " updated_at = now()",
        userId,
        cityId,
        status.name());
  }

  void delete(UUID userId, UUID cityId) {
    jdbcTemplate.update(
        "DELETE FROM user_city_pin WHERE user_id = ? AND city_id = ?", userId, cityId);
  }

  List<CityPin> findByUser(UUID userId) {
    return jdbcTemplate.query(
        "SELECT p.status, c.id, c.name, c.latitude, c.longitude, co.code"
            + " FROM user_city_pin p JOIN city c ON c.id = p.city_id"
            + " JOIN country co ON co.id = c.country_id"
            + " WHERE p.user_id = ? ORDER BY c.name",
        (rs, rowNum) ->
            new CityPin(
                new City(
                    rs.getObject("id", UUID.class),
                    rs.getString("name"),
                    rs.getBigDecimal("latitude"),
                    rs.getBigDecimal("longitude"),
                    rs.getString("code")),
                PinStatus.valueOf(rs.getString("status"))),
        userId);
  }
}
