package com.matchmap.api.geo;

import com.matchmap.api.account.RequiresAdmin;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CatalogImportService {

  private static final List<String> NAME_LANGUAGES =
      List.of("es", "de", "fr", "ja", "zh-Hant", "ko");
  private static final Pattern CODE = Pattern.compile("[A-Z]{2}");
  private static final Pattern CITY_KEY = Pattern.compile("[a-z0-9][a-z0-9-]{0,49}");
  private static final List<String> COUNTRY_HEADER = List.of("code", "name");
  private static final List<String> NAME_HEADER = List.of("code", "lang", "name");
  private static final List<String> CITY_HEADER =
      List.of("city_key", "country_code", "name", "latitude", "longitude");

  private final JdbcTemplate jdbcTemplate;

  CatalogImportService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  record CountryRow(String code, String name) {}

  record NameRow(String code, String lang, String name) {}

  record CityRow(
      String key, String countryCode, String name, BigDecimal latitude, BigDecimal longitude) {}

  @RequiresAdmin
  @Transactional
  CatalogImportResult importCatalog(CatalogImportInput input) {
    List<CatalogImportError> errors = new ArrayList<>();
    List<CountryRow> countries = readCountries(input.countries(), errors);
    Set<String> countryCodes = new HashSet<>();
    countries.forEach(country -> countryCodes.add(country.code()));
    List<NameRow> names = readNames(input.countryNames(), countryCodes, errors);
    List<CityRow> cities = readCities(input.cities(), countryCodes, errors);
    if (!errors.isEmpty()) {
      return new CatalogImportResult(0, 0, 0, List.of(), errors);
    }
    write(countries, names, cities);
    return new CatalogImportResult(
        countries.size(), names.size(), cities.size(), missingNames(countries, names), List.of());
  }

  private List<CountryRow> readCountries(String text, List<CatalogImportError> errors) {
    List<CountryRow> result = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (CsvParser.Row row : rows("countries", text, COUNTRY_HEADER, errors)) {
      String code = row.fields().get(0);
      String name = row.fields().get(1);
      if (!CODE.matcher(code).matches()) {
        errors.add(
            new CatalogImportError("countries", row.line(), "code must be two uppercase letters"));
      } else if (!validText(name, 100)) {
        errors.add(
            new CatalogImportError("countries", row.line(), "name must be 1 to 100 characters"));
      } else if (!seen.add(code)) {
        errors.add(new CatalogImportError("countries", row.line(), "code is repeated in the file"));
      } else {
        result.add(new CountryRow(code, name));
      }
    }
    return result;
  }

  private List<NameRow> readNames(
      String text, Set<String> countryCodes, List<CatalogImportError> errors) {
    List<NameRow> result = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (CsvParser.Row row : rows("country_names", text, NAME_HEADER, errors)) {
      String code = row.fields().get(0);
      String lang = row.fields().get(1);
      String name = row.fields().get(2);
      if (!countryCodes.contains(code)) {
        errors.add(
            new CatalogImportError(
                "country_names", row.line(), "code is not in the countries file"));
      } else if (!NAME_LANGUAGES.contains(lang)) {
        errors.add(
            new CatalogImportError(
                "country_names", row.line(), "lang is not a supported language"));
      } else if (!validText(name, 100)) {
        errors.add(
            new CatalogImportError(
                "country_names", row.line(), "name must be 1 to 100 characters"));
      } else if (!seen.add(code + ":" + lang)) {
        errors.add(
            new CatalogImportError(
                "country_names", row.line(), "code and lang are repeated in the file"));
      } else {
        result.add(new NameRow(code, lang, name));
      }
    }
    return result;
  }

  private List<CityRow> readCities(
      String text, Set<String> countryCodes, List<CatalogImportError> errors) {
    List<CityRow> result = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (CsvParser.Row row : rows("cities", text, CITY_HEADER, errors)) {
      String key = row.fields().get(0);
      String countryCode = row.fields().get(1);
      String name = row.fields().get(2);
      BigDecimal latitude = coordinate(row.fields().get(3), 90);
      BigDecimal longitude = coordinate(row.fields().get(4), 180);
      String problem;
      if (!CITY_KEY.matcher(key).matches()) {
        problem = "city_key must be lowercase letters, digits and hyphens";
      } else if (!countryCodes.contains(countryCode)) {
        problem = "country_code is not in the countries file";
      } else if (!validText(name, 120)) {
        problem = "name must be 1 to 120 characters";
      } else if (latitude == null) {
        problem = "latitude must be a number from -90 to 90 with at most 6 decimals";
      } else if (longitude == null) {
        problem = "longitude must be a number from -180 to 180 with at most 6 decimals";
      } else if (!seen.add(key)) {
        problem = "city_key is repeated in the file";
      } else {
        result.add(new CityRow(key, countryCode, name, latitude, longitude));
        continue;
      }
      errors.add(new CatalogImportError("cities", row.line(), problem));
    }
    return result;
  }

  private List<CsvParser.Row> rows(
      String file, String text, List<String> header, List<CatalogImportError> errors) {
    List<CsvParser.Row> result = new ArrayList<>();
    List<CsvParser.Row> parsed = CsvParser.parse(text);
    if (parsed.isEmpty() || !header.equals(parsed.get(0).fields())) {
      errors.add(new CatalogImportError(file, 1, "header must be " + String.join(",", header)));
      return result;
    }
    for (CsvParser.Row row : parsed.subList(1, parsed.size())) {
      if (row.fields() == null) {
        errors.add(new CatalogImportError(file, row.line(), "line has an unclosed quote"));
      } else if (row.fields().size() != header.size()) {
        errors.add(
            new CatalogImportError(
                file, row.line(), "line must have " + header.size() + " fields"));
      } else {
        result.add(row);
      }
    }
    return result;
  }

  private static BigDecimal coordinate(String value, int limit) {
    try {
      BigDecimal number = new BigDecimal(value);
      boolean inRange = number.abs().compareTo(BigDecimal.valueOf(limit)) <= 0;
      boolean scaleFits = number.stripTrailingZeros().scale() <= 6;
      return inRange && scaleFits ? number : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static boolean validText(String value, int max) {
    return value != null && !value.isBlank() && value.length() <= max;
  }

  private static List<String> missingNames(List<CountryRow> countries, List<NameRow> names) {
    Set<String> present = new HashSet<>();
    names.forEach(name -> present.add(name.code() + ":" + name.lang()));
    List<String> missing = new ArrayList<>();
    for (CountryRow country : countries) {
      for (String lang : NAME_LANGUAGES) {
        if (!present.contains(country.code() + ":" + lang)) {
          missing.add(country.code() + ":" + lang);
        }
      }
    }
    return missing;
  }

  private void write(List<CountryRow> countries, List<NameRow> names, List<CityRow> cities) {
    jdbcTemplate.batchUpdate(
        "INSERT INTO country (id, code, name) VALUES (gen_random_uuid(), ?, ?)"
            + " ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name",
        countries.stream().map(country -> new Object[] {country.code(), country.name()}).toList());
    jdbcTemplate.batchUpdate(
        "INSERT INTO country_name (country_id, language, name)"
            + " SELECT id, ?, ? FROM country WHERE code = ?"
            + " ON CONFLICT (country_id, language) DO UPDATE SET name = EXCLUDED.name",
        names.stream().map(name -> new Object[] {name.lang(), name.name(), name.code()}).toList());
    jdbcTemplate.batchUpdate(
        "INSERT INTO city (id, city_key, country_id, name, latitude, longitude)"
            + " SELECT gen_random_uuid(), ?, id, ?, ?, ? FROM country WHERE code = ?"
            + " ON CONFLICT (city_key) DO UPDATE SET country_id = EXCLUDED.country_id,"
            + " name = EXCLUDED.name, latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude",
        cities.stream()
            .map(
                city ->
                    new Object[] {
                      city.key(), city.name(), city.latitude(), city.longitude(), city.countryCode()
                    })
            .toList());
  }
}
