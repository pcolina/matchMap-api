package com.matchmap.api.moderation;

import com.matchmap.api.account.RequiresAdmin;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ProhibitedWordAdminService {

  private final ProhibitedWordRepository words;
  private final WordCache wordCache;
  private final CandidateNormalizer normalizer = new CandidateNormalizer();

  ProhibitedWordAdminService(ProhibitedWordRepository words, WordCache wordCache) {
    this.words = words;
    this.wordCache = wordCache;
  }

  @RequiresAdmin
  @Transactional
  ProhibitedWord create(String word, String category, String language) {
    if (word == null || word.isBlank() || word.strip().length() > 100) {
      throw ModerationException.invalid("word", "Word must be 1 to 100 characters");
    }
    if (language == null || language.length() < 2 || language.length() > 5) {
      throw ModerationException.invalid("language", "Language must be 2 to 5 characters");
    }
    String normalized = normalizer.normalize(word.strip());
    if (normalized.isEmpty() || normalized.length() > 100) {
      throw ModerationException.invalid("word", "Word has no letters to match");
    }
    if (words.exists(normalized, category)) {
      throw ModerationException.duplicateWord();
    }
    UUID id = words.insert(word, normalized, category, language);
    wordCache.reload();
    return words.find(id);
  }

  @RequiresAdmin
  List<ProhibitedWord> list() {
    return words.findAll();
  }

  @RequiresAdmin
  @Transactional
  ProhibitedWord setActive(UUID id, boolean active) {
    if (!words.exists(id)) {
      throw ModerationException.invalid("id", "Word does not exist");
    }
    words.setActive(id, active);
    wordCache.reload();
    return words.find(id);
  }
}
