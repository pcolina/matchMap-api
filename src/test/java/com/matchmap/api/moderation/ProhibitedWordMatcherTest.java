package com.matchmap.api.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProhibitedWordMatcherTest {

  private final ProhibitedWordMatcher matcher =
      new ProhibitedWordMatcher(
          List.of(new WordEntry("nazis", "HATE", true), new WordEntry("ass", "HATE", true)));

  @Test
  void matchesLongWordInsideLongerCandidate() {
    assertThat(matcher.match("xnazisx")).contains("HATE");
  }

  @Test
  void doesNotMatchShortWordInsideLongerCandidate() {
    assertThat(matcher.match("classic")).isEmpty();
  }
}
