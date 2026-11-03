package com.matchmap.api.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class UsernameGeneratorTest {

  private static final String USERNAME_RULE = "[a-z0-9_]{3,30}";

  private final UsernameGenerator generator = new UsernameGenerator();

  @Test
  void replacesInvalidCharactersInLocalPart() {
    var username = generator.generate("Carla.Smith+news@gmail.com", taken());

    assertThat(username).isEqualTo("carla_smith_news");
  }

  @Test
  void addsNumericSuffixOnCollision() {
    var username = generator.generate("carla@gmail.com", taken("carla"));

    assertThat(username).isEqualTo("carla_2");
  }

  @Test
  void incrementsSuffixUntilFree() {
    var username = generator.generate("carla@gmail.com", taken("carla", "carla_2"));

    assertThat(username).isEqualTo("carla_3");
  }

  @Test
  void usesRandomFallbackWhenLocalPartIsTooShort() {
    var username = generator.generate("a@gmail.com", taken());

    assertThat(username).matches("user_[a-z0-9]{8}");
  }

  @Test
  void randomFallbackDiffersBetweenCalls() {
    var first = generator.generate("a@gmail.com", taken());
    var second = generator.generate("a@gmail.com", taken());

    assertThat(first).isNotEqualTo(second);
  }

  @Test
  void cutsLongLocalPartToThirtyCharacters() {
    var username = generator.generate("a".repeat(40) + "@gmail.com", taken());

    assertThat(username).hasSize(30).matches(USERNAME_RULE);
  }

  @Test
  void keepsSuffixWithinThirtyCharactersOnCollision() {
    var base = "a".repeat(30);

    var username = generator.generate(base + "@gmail.com", taken(base));

    assertThat(username).hasSize(30).endsWith("_2").matches(USERNAME_RULE);
    assertThat(username).startsWith("a".repeat(28));
  }

  private static Predicate<String> taken(String... names) {
    var set = Set.of(names);
    return set::contains;
  }
}
