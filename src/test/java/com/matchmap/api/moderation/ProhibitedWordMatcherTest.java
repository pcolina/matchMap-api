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

  @Test
  void ignoresInactiveEntries() {
    var inactive =
        new ProhibitedWordMatcher(
            List.of(new WordEntry("nazis", "HATE", false), new WordEntry("ass", "HATE", false)));

    assertThat(inactive.match("xnazisx")).isEmpty();
    assertThat(inactive.match("ass")).isEmpty();
  }

  @Test
  void matchesShortWordAsSeparatedSegment() {
    var segments = new ProhibitedWordMatcher(List.of(new WordEntry("nazi", "HATE", true)));

    assertThat(segments.match("ana_nazi")).contains("HATE");
  }

  @Test
  void matchesShortWordSplitBySeparators() {
    var segments = new ProhibitedWordMatcher(List.of(new WordEntry("nazi", "HATE", true)));

    assertThat(segments.match("n-a-z-i")).contains("HATE");
  }

  @Test
  void reportsNoMatchWhenNothingIsFound() {
    assertThat(matcher.match("hello")).isEmpty();
  }

  @Test
  void reportsTheCategoryOfTheMatchedWord() {
    assertThat(matcher.match("xnazisx")).contains("HATE");
  }

  @Test
  void matchesShortReservedWordAsWholeCandidate() {
    var normalized = new CandidateNormalizer().normalize("root");
    var reserved = new ProhibitedWordMatcher(List.of(new WordEntry(normalized, "RESERVED", true)));

    assertThat(reserved.match("r00t")).contains("RESERVED");
  }
}
