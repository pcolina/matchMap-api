package com.matchmap.api.geo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScenarioCoverageIT {

  private static final List<Path> SPECS =
      List.of(
          Path.of("openspec/specs/geo-catalog/spec.md"),
          Path.of("openspec/specs/travel-pins/spec.md"));
  private static final Path TESTS = Path.of("src/test/java");

  @Test
  @DisplayName("Every scenario of geo-catalog and travel-pins has a test that names it")
  void everyScenarioIsNamedByATest() throws IOException {
    List<String> scenarios = scenarioTitles();
    List<String> displayNames = displayNames();

    assertThat(scenarios).isNotEmpty();
    for (String scenario : scenarios) {
      assertThat(displayNames)
          .as("no test names the scenario '%s'", scenario)
          .anyMatch(name -> name.contains(scenario));
    }
  }

  private static List<String> scenarioTitles() throws IOException {
    List<String> titles = new ArrayList<>();
    for (Path file : SPECS) {
      for (String line : Files.readAllLines(file)) {
        if (line.startsWith("#### Scenario: ")) {
          titles.add(line.substring("#### Scenario: ".length()).trim());
        }
      }
    }
    return titles;
  }

  private static List<String> displayNames() throws IOException {
    List<String> names = new ArrayList<>();
    try (Stream<Path> files = Files.walk(TESTS)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        for (String line : Files.readAllLines(file)) {
          if (line.contains("@DisplayName(")) {
            names.add(line);
          }
        }
      }
    }
    return names;
  }
}
