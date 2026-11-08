package com.matchmap.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "MATCHMAP_REFRESH_COOKIE_SAMESITE=None")
class RefreshCookieConfigurationIT {

  @Autowired Environment environment;

  @Test
  void cookieSameSiteComesFromConfiguration() {
    assertThat(environment.getProperty("matchmap.auth.refresh-cookie.same-site")).isEqualTo("None");
  }
}
