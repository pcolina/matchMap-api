package com.matchmap.api.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
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

  @Test
  void fallbackGivesUpAfterFiveAttempts() {
    var attempts = new AtomicInteger();
    var generator = new UsernameGenerator(new Random(7));

    var result =
        generator.fallbackUsername(
            name -> {
              if (attempts.incrementAndGet() > 100) {
                throw new AssertionError(
                    "the fallback does not stop after a fixed number of attempts");
              }
              return true;
            });

    assertThat(result).isEmpty();
    assertThat(attempts).hasValue(5);
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
