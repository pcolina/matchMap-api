package com.matchmap.api.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class FallbackUsernameTest {

  private final UsernameGenerator generator = new UsernameGenerator();

  @Test
  void fallbackIsUserPrefixAndEightRandomCharacters() {
    assertThat(generator.fallbackUsername()).matches("user_[a-z0-9]{8}");
  }

  @Test
  void fallbackFitsTheUsernameLengthLimit() {
    assertThat(generator.fallbackUsername()).hasSizeLessThanOrEqualTo(30);
  }

  @Test
  void fallbackRetriesAfterACollision() {
    var firstTakenThenFree =
        new UsernameGenerator(sequence(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1));

    assertThat(firstTakenThenFree.fallbackUsername(name -> name.equals("user_aaaaaaaa")))
        .contains("user_bbbbbbbb");
  }

  private static Random sequence(int... values) {
    var iterator = IntStream.of(values).iterator();
    return new Random() {
      @Override
      public int nextInt(int bound) {
        return iterator.nextInt();
      }
    };
  }
}
