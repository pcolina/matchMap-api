package com.matchmap.api.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PersistenceIT {

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void userAccountHasNoDisplayNameColumn() {
    Integer columns =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM information_schema.columns"
                + " WHERE table_name = 'user_account' AND column_name = 'display_name'",
            Integer.class);

    assertThat(columns).isZero();
  }
}
