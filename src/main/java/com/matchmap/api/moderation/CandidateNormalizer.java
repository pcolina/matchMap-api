package com.matchmap.api.moderation;

import java.text.Normalizer;
import java.util.Locale;

final class CandidateNormalizer {

  String normalize(String candidate) {
    var text = Normalizer.normalize(candidate, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    text = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    text = mapLeetspeak(text);
    text = text.replaceAll("[._\\- ]", "");
    return text.replaceAll("(\\p{L})\\1+", "$1");
  }

  private String mapLeetspeak(String text) {
    return text.replace('0', 'o')
        .replace('1', 'i')
        .replace('3', 'e')
        .replace('4', 'a')
        .replace('5', 's')
        .replace('7', 't')
        .replace('@', 'a')
        .replace('$', 's');
  }
}
