package com.matchmap.api.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProhibitedWordScenarioCoverageIT {

  private static final Path SPEC =
      Path.of("openspec/changes/add-prohibited-word-admin/specs/prohibited-word-admin/spec.md");
  private static final Path TESTS = Path.of("src/test/java");

  @Test
  @DisplayName("Every scenario of prohibited-word-admin has a test that names it")
  void everyScenarioIsNamedByATest() throws IOException {
    List<String> scenarios = new ArrayList<>();
    for (String line : Files.readAllLines(SPEC)) {
      if (line.startsWith("#### Scenario: ")) {
        scenarios.add(line.substring("#### Scenario: ".length()).trim());
      }
    }
    List<String> displayNames = new ArrayList<>();
    try (Stream<Path> files = Files.walk(TESTS)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        for (String line : Files.readAllLines(file)) {
          if (line.contains("@DisplayName(")) {
            displayNames.add(line);
          }
        }
      }
    }

    assertThat(scenarios).hasSize(8);
    for (String scenario : scenarios) {
      assertThat(displayNames)
          .as("no test names the scenario '%s'", scenario)
          .anyMatch(name -> name.contains(scenario));
    }
  }
}
