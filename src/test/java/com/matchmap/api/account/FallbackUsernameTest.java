package com.matchmap.api.account;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FallbackUsernameTest {

  private final UsernameGenerator generator = new UsernameGenerator();

  @Test
  void fallbackIsUserPrefixAndEightRandomCharacters() {
    assertThat(generator.fallbackUsername()).matches("user_[a-z0-9]{8}");
  }

  @Test
  void fallbackFitsTheUsernameLengthLimit() {
    assertThat(generator.fallbackUsername()).hasSizeLessThanOrEqualTo(30);
  }
}
