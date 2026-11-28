package com.matchmap.api.geo;

import java.util.List;

record CatalogImportResult(
    int countriesLoaded,
    int namesLoaded,
    int citiesLoaded,
    List<String> missingNames,
    List<CatalogImportError> errors) {}
