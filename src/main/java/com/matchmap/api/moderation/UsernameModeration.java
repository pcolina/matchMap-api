package com.matchmap.api.moderation;

import org.springframework.stereotype.Component;

@Component
public class UsernameModeration {

  private final WordCache wordCache;

  public UsernameModeration(WordCache wordCache) {
    this.wordCache = wordCache;
  }

  public boolean isBlocked(String username) {
    return wordCache.current().isBlocked(username);
  }
}
