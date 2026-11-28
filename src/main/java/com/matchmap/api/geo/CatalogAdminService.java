package com.matchmap.api.geo;

import com.matchmap.api.account.RequiresAdmin;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CatalogAdminService {

  private static final Pattern CODE = Pattern.compile("[A-Z]{2}");
  private static final Pattern CITY_KEY = Pattern.compile("[a-z0-9][a-z0-9-]{0,49}");

  private final JdbcTemplate jdbcTemplate;
  private final CountryCatalog countries;

  CatalogAdminService(JdbcTemplate jdbcTemplate, CountryCatalog countries) {
    this.jdbcTemplate = jdbcTemplate;
    this.countries = countries;
  }

  @RequiresAdmin
  @Transactional
  Country createCountry(String code, String name) {
    if (code == null || !CODE.matcher(code).matches()) {
      throw GeoException.invalid("code", "Country code must be two uppercase letters");
    }
    if (!validText(name, 100)) {
      throw GeoException.invalid("name", "Country name must be 1 to 100 characters");
    }
    Integer existing =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM country WHERE code = ?", Integer.class, code);
    if (existing != 0) {
      throw GeoException.invalid("code", "Country code is already in use");
    }
    UUID id =
        jdbcTemplate.queryForObject(
            "INSERT INTO country (id, code, name) VALUES (gen_random_uuid(), ?, ?) RETURNING id",
            UUID.class,
            code,
            name);
    return countries.findById(id);
  }

  @RequiresAdmin
  @Transactional
  City createCity(UUID countryId, String cityKey, String name, Double latitude, Double longitude) {
    List<Boolean> countryRows =
        jdbcTemplate.query(
            "SELECT successor_id IS NULL FROM country WHERE id = ?",
            (rs, rowNum) -> rs.getBoolean(1),
            countryId);
    if (countryRows.isEmpty()) {
      throw GeoException.invalid("countryId", "Country does not exist");
    }
    if (!countryRows.get(0)) {
      throw GeoException.invalid("countryId", "Country is merged and accepts no new cities");
    }
    if (cityKey == null || !CITY_KEY.matcher(cityKey).matches()) {
      throw GeoException.invalid(
          "cityKey", "City key must be lowercase letters, digits and hyphens");
    }
    Integer keys =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM city WHERE city_key = ?", Integer.class, cityKey);
    if (keys != 0) {
      throw GeoException.invalid("cityKey", "City key is already in use");
    }
    if (!validText(name, 120)) {
      throw GeoException.invalid("name", "City name must be 1 to 120 characters");
    }
    BigDecimal lat = coordinate(latitude, 90);
    if (lat == null) {
      throw GeoException.invalid(
          "latitude", "Latitude must be a number from -90 to 90 with at most 6 decimals");
    }
    BigDecimal lon = coordinate(longitude, 180);
    if (lon == null) {
      throw GeoException.invalid(
          "longitude", "Longitude must be a number from -180 to 180 with at most 6 decimals");
    }
    UUID id =
        jdbcTemplate.queryForObject(
            "INSERT INTO city (id, city_key, country_id, name, latitude, longitude)"
                + " VALUES (gen_random_uuid(), ?, ?, ?, ?, ?) RETURNING id",
            UUID.class,
            cityKey,
            countryId,
            name,
            lat,
            lon);
    return new City(id, name, lat, lon, countryCode(countryId));
  }

  @RequiresAdmin
  @Transactional
  City retireCity(UUID cityId) {
    Integer found =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM city WHERE id = ?", Integer.class, cityId);
    if (found == 0) {
      throw GeoException.invalid("cityId", "City does not exist");
    }
    jdbcTemplate.update("UPDATE city SET active = false WHERE id = ?", cityId);
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

  @RequiresAdmin
  @Transactional
  Country mergeCountry(UUID countryId, UUID successorId) {
    if (countryId.equals(successorId)) {
      throw GeoException.invalid("successorId", "A country cannot merge into itself");
    }
    Integer country =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM country WHERE id = ?", Integer.class, countryId);
    if (country == 0) {
      throw GeoException.invalid("countryId", "Country does not exist");
    }
    Integer successor =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM country WHERE id = ?", Integer.class, successorId);
    if (successor == 0) {
      throw GeoException.invalid("successorId", "Successor does not exist");
    }
    jdbcTemplate.update(
        "UPDATE city SET country_id = ? WHERE country_id = ?", successorId, countryId);
    jdbcTemplate.update("UPDATE country SET successor_id = ? WHERE id = ?", successorId, countryId);
    return countries.findById(countryId);
  }

  private String countryCode(UUID countryId) {
    return jdbcTemplate.queryForObject(
        "SELECT code FROM country WHERE id = ?", String.class, countryId);
  }

  private static boolean validText(String value, int max) {
    return value != null && !value.isBlank() && value.length() <= max;
  }

  private static BigDecimal coordinate(Double value, int limit) {
    if (value == null || Double.isNaN(value) || Math.abs(value) > limit) {
      return null;
    }
    BigDecimal number = BigDecimal.valueOf(value);
    return number.stripTrailingZeros().scale() <= 6 ? number : null;
  }
}
