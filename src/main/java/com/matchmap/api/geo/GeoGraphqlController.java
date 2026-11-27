package com.matchmap.api.geo;

import java.util.List;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
class GeoGraphqlController {

  private final CountryCatalog countries;

  GeoGraphqlController(CountryCatalog countries) {
    this.countries = countries;
  }

  @QueryMapping
  List<Country> countries() {
    return countries.findAll();
  }
}
