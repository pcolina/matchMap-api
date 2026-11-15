package com.matchmap.api.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

class WordCacheTest {

  @Test
  void signUpWaitsForFirstLoad() {
    var source = new FakeSource();
    source.prohibited = List.of(new WordEntry("nazism", "HATE", true));

    var cache = new WordCache(source);

    assertThat(source.loads).isEqualTo(1);
    assertThat(cache.current().isBlocked("nazism_fan")).isTrue();
  }

  @Test
  void reloadReplacesWholeSnapshot() {
    var source = new FakeSource();
    source.prohibited = List.of(new WordEntry("nazism", "HATE", true));
    var cache = new WordCache(source);
    var oldSnapshot = cache.current();

    source.prohibited = List.of(new WordEntry("banedword", "HATE", true));
    cache.reload();

    assertThat(oldSnapshot.isBlocked("nazism_fan")).isTrue();
    assertThat(oldSnapshot.isBlocked("bannedword_fan")).isFalse();
    assertThat(cache.current().isBlocked("nazism_fan")).isFalse();
    assertThat(cache.current().isBlocked("bannedword_fan")).isTrue();
  }

  @Test
  void failedReloadKeepsLastGoodSnapshot() {
    var source = new FakeSource();
    source.prohibited = List.of(new WordEntry("nazism", "HATE", true));
    var cache = new WordCache(source);
    source.failing = true;
    var logger = (Logger) LoggerFactory.getLogger(WordCache.class);
    var appender = new ListAppender<ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    try {
      assertThatCode(cache::reload).doesNotThrowAnyException();

      assertThat(cache.current().isBlocked("nazism_fan")).isTrue();
      assertThat(appender.list)
          .anySatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.WARN))
          .allSatisfy(event -> assertThat(event.getFormattedMessage()).doesNotContain("nazism"));
    } finally {
      logger.detachAppender(appender);
    }
  }

  @Test
  void reloadLogsCountsOnly() {
    var source = new FakeSource();
    source.prohibited = List.of(new WordEntry("nazism", "HATE", true));
    source.reserved = List.of(new WordEntry("rot", "RESERVED", true));
    var cache = new WordCache(source);
    var logger = (Logger) LoggerFactory.getLogger(WordCache.class);
    var appender = new ListAppender<ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    try {
      cache.reload();

      assertThat(appender.list)
          .anySatisfy(event -> assertThat(event.getFormattedMessage()).contains("1").contains("1"))
          .allSatisfy(
              event ->
                  assertThat(event.getFormattedMessage())
                      .doesNotContain("nazism")
                      .doesNotContain("rot"));
    } finally {
      logger.detachAppender(appender);
    }
  }

  @Test
  void emptySnapshotKeepsUsernameAndRequestsReload() {
    var source = new FakeSource();
    var cache = new WordCache(source);
    var loadsAfterStartup = source.loads;

    assertThat(cache.current().isBlocked("ana")).isFalse();
    assertThat(source.loads).isEqualTo(loadsAfterStartup + 1);
  }

  @Test
  void scheduledMethodCallsReload() throws NoSuchMethodException {
    Method scheduled = WordCache.class.getDeclaredMethod("scheduledReload");
    var annotation = scheduled.getAnnotation(Scheduled.class);
    var source = new FakeSource();
    var cache = new WordCache(source);
    var loadsAfterStartup = source.loads;

    assertThat(annotation.fixedRate()).isEqualTo(3_600_000L);
    cache.scheduledReload();
    assertThat(source.loads).isEqualTo(loadsAfterStartup + 1);
  }

  private static final class FakeSource implements WordEntrySource {

    List<WordEntry> prohibited = new ArrayList<>();
    List<WordEntry> reserved = new ArrayList<>();
    int loads;
    boolean failing;

    @Override
    public WordLists load() {
      loads++;
      if (failing) {
        throw new IllegalStateException("database unreachable");
      }
      return new WordLists(prohibited, reserved);
    }
  }
}
