package com.matchmap.api.geo;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
class GeoGraphqlController {

  private final CountryCatalog countries;
  private final CityCatalog cities;
  private final CatalogImportService catalogImport;
  private final CityPinService pins;

  private final CatalogAdminService admin;

  GeoGraphqlController(
      CountryCatalog countries,
      CityCatalog cities,
      CatalogImportService catalogImport,
      CityPinService pins,
      CatalogAdminService admin) {
    this.countries = countries;
    this.cities = cities;
    this.catalogImport = catalogImport;
    this.pins = pins;
    this.admin = admin;
  }

  @MutationMapping
  Country createCountry(@Argument String code, @Argument String name) {
    return admin.createCountry(code, name);
  }

  @MutationMapping
  City createCity(
      @Argument String countryId,
      @Argument String cityKey,
      @Argument String name,
      @Argument Double latitude,
      @Argument Double longitude) {
    return admin.createCity(parseId(countryId, "countryId"), cityKey, name, latitude, longitude);
  }

  @MutationMapping
  City retireCity(@Argument String cityId) {
    return admin.retireCity(parseId(cityId, "cityId"));
  }

  @MutationMapping
  Country mergeCountry(@Argument String countryId, @Argument String successorId) {
    return admin.mergeCountry(parseId(countryId, "countryId"), parseId(successorId, "successorId"));
  }

  private static UUID parseId(String value, String field) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      throw GeoException.invalid(field, "Invalid id");
    }
  }

  @SchemaMapping(typeName = "Country", field = "status")
  PinStatus status(Country country, Principal principal) {
    return principal == null ? null : pins.statusInCountry(userId(principal), country.code());
  }

  @SchemaMapping(typeName = "Country", field = "visitedCities")
  List<City> visitedCities(Country country, Principal principal) {
    return principal == null ? List.of() : pins.visitedCitiesIn(userId(principal), country.code());
  }

  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  List<CityPin> cityPins(Principal principal) {
    return pins.pinsOf(userId(principal));
  }

  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  CityPin setCityPin(Principal principal, @Argument String cityId, @Argument PinStatus status) {
    return pins.setPin(userId(principal), cityId(cityId), status);
  }

  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  boolean removeCityPin(Principal principal, @Argument String cityId) {
    return pins.removePin(userId(principal), cityId(cityId));
  }

  private static UUID userId(Principal principal) {
    return UUID.fromString(principal.getName());
  }

  private static UUID cityId(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      throw GeoException.invalidCityId();
    }
  }

  @QueryMapping
  List<Country> countries() {
    return countries.findAll();
  }

  @SchemaMapping(typeName = "Country", field = "cities")
  List<City> cities(Country country) {
    return cities.findActiveByCountryCode(country.code());
  }

  @SchemaMapping(typeName = "City", field = "country")
  Country country(City city) {
    return countries.findByCode(city.countryCode());
  }

  @MutationMapping
  CatalogImportResult importCatalog(@Argument CatalogImportInput input) {
    return catalogImport.importCatalog(input);
  }
}
