package com.matchmap.api.account;

import static com.matchmap.api.support.GraphqlRequests.registerBody;
import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class FirstAdministratorIT {

  private static final Path RUNBOOK = Path.of("docs/operations/first-administrator.md");
  private static final String EMAIL_PLACEHOLDER = "<email>";

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void documentedCommandSetsAdminRole() throws Exception {
    String email = "operator" + uniqueSuffix() + "@example.com";
    mockMvc
        .post()
        .uri("/graphql")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(registerBody(email, "Correcthorse1!"))
        .exchange();

    String command = documentedCommand().replace(EMAIL_PLACEHOLDER, email);
    jdbcTemplate.execute(command);

    String role =
        jdbcTemplate.queryForObject(
            "SELECT role FROM user_account WHERE email = ?", String.class, email);
    assertThat(role).isEqualTo("ADMIN");
  }

  private String documentedCommand() throws Exception {
    return Files.readAllLines(RUNBOOK).stream()
        .filter(line -> line.startsWith("UPDATE user_account"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No UPDATE command documented in " + RUNBOOK));
  }
}
