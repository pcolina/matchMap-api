package com.matchmap.api.archive;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArchiveCheckTest {

  @TempDir Path root;

  @Test
  void changeStillOpen() throws IOException {
    Files.createDirectories(root.resolve("openspec/changes/add-x"));

    var result = new ArchiveCheck(root).evaluate("add-x");

    assertThat(result.passed()).isFalse();
    assertThat(result.output()).contains("openspec/changes/add-x");
  }

  @Test
  void changeArchived() throws IOException {
    Files.createDirectories(root.resolve("openspec/changes/archive/2026-11-03-add-x"));

    var result = new ArchiveCheck(root).evaluate("add-x");

    assertThat(result.passed()).isTrue();
  }

  @Test
  void archiveMissing() {
    var result = new ArchiveCheck(root).evaluate("add-x");

    assertThat(result.passed()).isFalse();
    assertThat(result.output()).contains("not archived");
  }
}
