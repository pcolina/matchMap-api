package com.matchmap.api.moderation;

import java.util.stream.Stream;

final class WordSnapshot {

  private final ProhibitedWordMatcher matcher;
  private final boolean empty;

  WordSnapshot(WordLists lists) {
    var entries = Stream.concat(lists.prohibited().stream(), lists.reserved().stream()).toList();
    this.matcher = new ProhibitedWordMatcher(entries);
    this.empty = entries.stream().noneMatch(WordEntry::active);
  }

  boolean isEmpty() {
    return empty;
  }

  boolean isBlocked(String username) {
    return matcher.match(username).isPresent();
  }
}
