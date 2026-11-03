package com.matchmap.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class AuthContractTest {

  @Test
  void declaresAuthEndpoints() throws IOException {
    Map<String, Object> contract;
    try (InputStream in = Files.newInputStream(Path.of("contracts/openapi/auth.yaml"))) {
      contract = new Yaml().load(in);
    }

    assertThat(String.valueOf(contract.get("openapi"))).startsWith("3.1");
    assertThat(responseCodes(contract, "/api/auth/login")).contains("200", "401", "429");
    assertThat(responseCodes(contract, "/api/auth/google")).contains("200", "401", "403");
    assertThat(responseCodes(contract, "/api/auth/refresh")).contains("200", "401");
    assertThat(responseCodes(contract, "/api/auth/logout")).contains("204");
  }

  @SuppressWarnings("unchecked")
  private static java.util.Set<String> responseCodes(Map<String, Object> contract, String path) {
    var paths = (Map<String, Object>) contract.get("paths");
    var operation = (Map<String, Object>) ((Map<String, Object>) paths.get(path)).get("post");
    var responses = (Map<Object, Object>) operation.get("responses");
    var codes = new java.util.HashSet<String>();
    responses.keySet().forEach(code -> codes.add(String.valueOf(code)));
    return codes;
  }
}
