package com.matchmap.api.geo;

import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
class GeoGraphqlController {

  private final CountryCatalog countries;
  private final CatalogImportService catalogImport;

  GeoGraphqlController(CountryCatalog countries, CatalogImportService catalogImport) {
    this.countries = countries;
    this.catalogImport = catalogImport;
  }

  @QueryMapping
  List<Country> countries() {
    return countries.findAll();
  }

  @MutationMapping
  CatalogImportResult importCatalog(@Argument CatalogImportInput input) {
    return catalogImport.importCatalog(input);
  }
}
