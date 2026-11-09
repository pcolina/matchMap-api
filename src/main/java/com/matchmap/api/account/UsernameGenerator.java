package com.matchmap.api.account;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

@Component
class UsernameGenerator {

  private static final int MAX_LENGTH = 30;
  private static final int MIN_BASE_LENGTH = 3;
  private static final int RANDOM_LENGTH = 8;
  private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
  private static final SecureRandom RANDOM = new SecureRandom();

  String generate(String email, Predicate<String> taken) {
    return generateFrom(email.substring(0, email.indexOf('@')), taken);
  }

  String generateFromName(String name, Predicate<String> taken) {
    return generateFrom(name, taken);
  }

  private static String generateFrom(String source, Predicate<String> taken) {
    var localPart = source.toLowerCase(Locale.ROOT);
    var sanitized = localPart.replaceAll("[^a-z0-9_]", "_");
    var base = sanitized.length() < MIN_BASE_LENGTH ? "user_" + randomSuffix() : sanitized;
    var candidate = truncate(base, MAX_LENGTH);
    for (var n = 2; taken.test(candidate); n++) {
      var suffix = "_" + n;
      candidate = truncate(base, MAX_LENGTH - suffix.length()) + suffix;
    }
    return candidate;
  }

  private static String truncate(String value, int length) {
    return value.length() <= length ? value : value.substring(0, length);
  }

  private static String randomSuffix() {
    var builder = new StringBuilder(RANDOM_LENGTH);
    for (var i = 0; i < RANDOM_LENGTH; i++) {
      builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
    }
    return builder.toString();
  }
}
