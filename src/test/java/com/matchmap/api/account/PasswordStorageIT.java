package com.matchmap.api.account;

import static com.matchmap.api.support.GraphqlRequests.registerBody;
import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PasswordStorageIT {

  private static final String PASSWORD = "correct horse battery";

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void storesSaltedHashOnly() {
    String first = uniqueSuffix();
    String second = uniqueSuffix();
    graphql(registerBody("hash" + first + "@example.com", "hash_" + first, "Ana", PASSWORD));
    graphql(registerBody("hash" + second + "@example.com", "hash_" + second, "Bob", PASSWORD));

    List<String> hashes =
        jdbcTemplate.queryForList(
            "SELECT password_hash FROM user_account WHERE username IN (?, ?)",
            String.class,
            "hash_" + first,
            "hash_" + second);

    assertThat(hashes)
        .hasSize(2)
        .doesNotHaveDuplicates()
        .allSatisfy(
            hash ->
                assertThat(hash)
                    .startsWith("{argon2@SpringSecurity_v5_8}")
                    .doesNotContain(PASSWORD));
  }

  private MvcTestResult graphql(String requestBody) {
    return mockMvc
        .post()
        .uri("/graphql")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(requestBody)
        .exchange();
  }
}
