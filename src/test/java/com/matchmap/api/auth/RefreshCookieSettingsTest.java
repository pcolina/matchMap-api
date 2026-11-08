package com.matchmap.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RefreshCookieSettingsTest {

  @ParameterizedTest
  @ValueSource(strings = {"Strict", "Lax", "None"})
  void acceptedSameSiteValues(String value) {
    assertThat(new RefreshCookieProperties(value).sameSite()).isEqualTo(value);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Lxa", "none", "LAX", ""})
  void refusedSameSiteValues(String value) {
    assertThatThrownBy(() -> new RefreshCookieProperties(value))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("MATCHMAP_REFRESH_COOKIE_SAMESITE");
  }
}
