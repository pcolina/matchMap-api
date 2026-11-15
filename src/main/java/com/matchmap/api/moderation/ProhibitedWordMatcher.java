package com.matchmap.api.moderation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;

final class ProhibitedWordMatcher {

  private static final int MAX_WHOLE_WORD_LENGTH = 4;
  private static final String SEPARATORS = "[._\\- ]";

  private final CandidateNormalizer normalizer = new CandidateNormalizer();
  private final Map<String, String> categoryByWord = new HashMap<>();
  private final Trie substrings;

  ProhibitedWordMatcher(List<WordEntry> entries) {
    var longWords = new ArrayList<String>();
    for (var entry : entries) {
      if (!entry.active()) {
        continue;
      }
      categoryByWord.putIfAbsent(entry.normalized(), entry.category());
      if (entry.normalized().length() > MAX_WHOLE_WORD_LENGTH) {
        longWords.add(entry.normalized());
      }
    }
    this.substrings = longWords.isEmpty() ? null : Trie.builder().addKeywords(longWords).build();
  }

  Optional<String> match(String candidate) {
    var substring = matchSubstring(normalizer.normalize(candidate));
    if (substring.isPresent()) {
      return substring;
    }
    return Stream.concat(Stream.of(candidate), Arrays.stream(candidate.split(SEPARATORS)))
        .map(normalizer::normalize)
        .filter(form -> form.length() <= MAX_WHOLE_WORD_LENGTH)
        .map(categoryByWord::get)
        .filter(Objects::nonNull)
        .findFirst();
  }

  private Optional<String> matchSubstring(String normalized) {
    if (substrings != null) {
      Emit emit = substrings.firstMatch(normalized);
      if (emit != null) {
        return Optional.of(categoryByWord.get(emit.getKeyword()));
      }
    }
    return Optional.empty();
  }
}
