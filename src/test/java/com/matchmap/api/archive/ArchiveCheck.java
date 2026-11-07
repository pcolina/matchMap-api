package com.matchmap.api.archive;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

class ArchiveCheck {

  private static final String CHANGES = "openspec/changes";

  private final Path root;

  ArchiveCheck(Path root) {
    this.root = root;
  }

  Result evaluate(String changeName) {
    if (Files.isDirectory(root.resolve(CHANGES).resolve(changeName))) {
      return new Result(false, "Change still open: " + CHANGES + "/" + changeName + "/");
    }
    if (isArchived(changeName)) {
      return new Result(true, "Change archived: " + changeName);
    }
    if (changeName.startsWith("fix-") || changeName.startsWith("bump-")) {
      return new Result(true, "Pull request without a change: no change is involved");
    }
    return new Result(
        false, "Change not archived: no " + CHANGES + "/archive/<date>-" + changeName + "/");
  }

  private boolean isArchived(String changeName) {
    var archive = root.resolve(CHANGES).resolve("archive");
    if (!Files.isDirectory(archive)) {
      return false;
    }
    var pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}-" + Pattern.quote(changeName));
    try (Stream<Path> entries = Files.list(archive)) {
      return entries
          .map(entry -> entry.getFileName().toString())
          .anyMatch(name -> pattern.matcher(name).matches());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  record Result(boolean passed, String output) {}
}
