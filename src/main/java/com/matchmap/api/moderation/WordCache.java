package com.matchmap.api.moderation;

import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WordCache {

  private static final Logger LOG = LoggerFactory.getLogger(WordCache.class);

  private final WordEntrySource source;
  private final AtomicReference<WordSnapshot> snapshot;

  WordCache(WordEntrySource source) {
    this.source = source;
    this.snapshot = new AtomicReference<>(new WordSnapshot(source.load()));
  }

  WordSnapshot current() {
    var current = snapshot.get();
    if (current.isEmpty()) {
      reload();
      return snapshot.get();
    }
    return current;
  }

  public void reload() {
    try {
      var lists = source.load();
      snapshot.set(new WordSnapshot(lists));
      LOG.info(
          "Word lists reloaded: prohibited={} reserved={}",
          lists.prohibited().size(),
          lists.reserved().size());
    } catch (RuntimeException e) {
      LOG.warn(
          "Word list reload failed, keeping the last loaded lists: {}",
          e.getClass().getSimpleName());
    }
  }

  @Scheduled(fixedRate = 3_600_000L)
  void scheduledReload() {
    reload();
  }
}
