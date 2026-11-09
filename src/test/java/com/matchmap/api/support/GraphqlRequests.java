package com.matchmap.api.support;

import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

public final class GraphqlRequests {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  public static final String REGISTER =
      """
      mutation Register($input: RegisterInput!) {
        register(input: $input) { id email username createdAt }
      }
      """;

  private GraphqlRequests() {}

  public static String body(String query) {
    return JSON.writeValueAsString(Map.of("query", query));
  }

  public static String body(String query, Map<String, Object> variables) {
    return JSON.writeValueAsString(Map.of("query", query, "variables", variables));
  }

  public static String registerBody(String email, String password) {
    return body(REGISTER, Map.of("input", Map.of("email", email, "password", password)));
  }

  public static String uniqueSuffix() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
  }
}
