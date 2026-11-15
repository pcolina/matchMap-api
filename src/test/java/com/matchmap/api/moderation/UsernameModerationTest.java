package com.matchmap.api.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class UsernameModerationTest {

  @Test
  void signUpDoesNotQueryWordTables() {
    var loads = new AtomicInteger();
    WordEntrySource source =
        () -> {
          loads.incrementAndGet();
          return new WordLists(List.of(new WordEntry("nazism", "HATE", true)), List.of());
        };
    var moderation = new UsernameModeration(new WordCache(source));
    var loadsAfterStartup = loads.get();

    assertThat(moderation.isBlocked("nazism_fan")).isTrue();
    assertThat(moderation.isBlocked("ana")).isFalse();
    assertThat(loads.get()).isEqualTo(loadsAfterStartup);
  }
}
