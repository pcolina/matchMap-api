package com.matchmap.api.account;

import static com.matchmap.api.support.GraphqlRequests.registerBody;
import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CatalogRoleIT {

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

  @Autowired CatalogChangeTestService catalogChange;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void administratorCanChangeTheCatalog() {
    String email = "admin" + uniqueSuffix() + "@example.com";
    String id = registerAccount(email);
    jdbcTemplate.update("UPDATE user_account SET role = 'ADMIN' WHERE id = ?::uuid", id);
    signIn(id);

    assertThat(catalogChange.change()).isEqualTo("ok");
  }

  private String registerAccount(String email) {
    var response =
        mockMvc
            .post()
            .uri("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(registerBody(email, "Correcthorse1!"))
            .exchange();
    return jdbcTemplate.queryForObject(
        "SELECT id::text FROM user_account WHERE email = ?", String.class, email);
  }

  private void signIn(String accountId) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(accountId, null, List.of()));
  }
}
