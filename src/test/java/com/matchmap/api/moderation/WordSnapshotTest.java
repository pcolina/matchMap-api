package com.matchmap.api.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class WordSnapshotTest {

  @Test
  void containsOnlyActiveEntries() {
    var snapshot =
        new WordSnapshot(
            new WordLists(
                List.of(
                    new WordEntry("nazism", "HATE", true),
                    new WordEntry("bannedword", "HATE", false)),
                List.of(new WordEntry("rot", "RESERVED", true))));

    assertThat(snapshot.isBlocked("nazism_fan")).isTrue();
    assertThat(snapshot.isBlocked("bannedword_fan")).isFalse();
    assertThat(snapshot.isBlocked("r00t")).isTrue();
  }

  @Test
  void matchesTheSameCandidatesAsBeforeTheCache() {
    var snapshot =
        new WordSnapshot(
            new WordLists(
                List.of(new WordEntry("nazism", "HATE", true)),
                List.of(new WordEntry("rot", "RESERVED", true))));

    assertThat(snapshot.isBlocked("n4zism_fan")).isTrue();
    assertThat(snapshot.isBlocked("r00t")).isTrue();
    assertThat(snapshot.isBlocked("ana.root")).isTrue();
    assertThat(snapshot.isBlocked("rooted")).isFalse();
  }
}
