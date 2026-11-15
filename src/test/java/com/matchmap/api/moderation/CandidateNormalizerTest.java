package com.matchmap.api.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CandidateNormalizerTest {

  private final CandidateNormalizer normalizer = new CandidateNormalizer();

  @Test
  void normalizesLeetspeak() {
    assertThat(normalizer.normalize("naz1")).isEqualTo("nazi");
  }

  @Test
  void removesSeparators() {
    assertThat(normalizer.normalize("n-a-z-i")).isEqualTo("nazi");
  }

  @Test
  void collapsesRepeatedLetters() {
    assertThat(normalizer.normalize("nnnazi")).isEqualTo("nazi");
  }

  @Test
  void normalizesCaseAndDiacritics() {
    assertThat(normalizer.normalize("NÁZI")).isEqualTo("nazi");
  }

  @Test
  void combinesDigitsAndLetters() {
    assertThat(normalizer.normalize("n4z1")).isEqualTo("nazi");
  }
}
