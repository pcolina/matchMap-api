package com.matchmap.api.graphql;

import org.springframework.boot.info.BuildProperties;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
class ApiVersionController {

  private final BuildProperties buildProperties;

  ApiVersionController(BuildProperties buildProperties) {
    this.buildProperties = buildProperties;
  }

  @QueryMapping
  String apiVersion() {
    return buildProperties.getVersion();
  }
}
