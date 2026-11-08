package com.matchmap.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RefreshCookieSettingsStartupTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(RefreshCookieSettingsConfiguration.class);

  @Test
  void unknownValueStopsStartup() {
    runner
        .withPropertyValues("matchmap.auth.refresh-cookie.same-site=Lxa")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasStackTraceContaining("MATCHMAP_REFRESH_COOKIE_SAMESITE");
            });
  }

  @EnableConfigurationProperties(RefreshCookieProperties.class)
  static class RefreshCookieSettingsConfiguration {}
}
