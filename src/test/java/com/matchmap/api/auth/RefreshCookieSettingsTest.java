package com.matchmap.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RefreshCookieSettingsTest {

  @ParameterizedTest
  @ValueSource(strings = {"Strict", "Lax", "None"})
  void acceptedSameSiteValues(String value) {
    assertThat(new RefreshCookieProperties(value).sameSite()).isEqualTo(value);
  }
}
