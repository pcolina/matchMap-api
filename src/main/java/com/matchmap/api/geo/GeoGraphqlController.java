package com.matchmap.api.geo;

import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
class GeoGraphqlController {

  private final CountryCatalog countries;
  private final CityCatalog cities;
  private final CatalogImportService catalogImport;

  GeoGraphqlController(
      CountryCatalog countries, CityCatalog cities, CatalogImportService catalogImport) {
    this.countries = countries;
    this.cities = cities;
    this.catalogImport = catalogImport;
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
