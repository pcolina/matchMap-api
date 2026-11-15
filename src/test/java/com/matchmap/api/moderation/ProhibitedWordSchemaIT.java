package com.matchmap.api.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ProhibitedWordSchemaIT {

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void createsEmptyWordTables() {
    assertThat(countRows("prohibited_word")).isZero();
    assertThat(countRows("reserved_word")).isZero();
  }

  private int countRows(String table) {
    return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
  }
}
