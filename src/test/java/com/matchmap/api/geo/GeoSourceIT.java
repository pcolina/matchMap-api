package com.matchmap.api.geo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeoSourceIT {

  private static final Path GEO_SOURCES = Path.of("src/main/java/com/matchmap/api/geo");
  private static final List<String> EXTERNAL_CLIENTS =
      List.of(
          "java.net.", "HttpClient", "RestTemplate", "RestClient", "WebClient", "URLConnection");

  @Test
  @DisplayName("Source is used only by the import")
  void geoCodeDoesNotCallExternalServices() throws IOException {
    try (Stream<Path> files = Files.list(GEO_SOURCES)) {
      files
          .filter(path -> path.toString().endsWith(".java"))
          .forEach(
              path -> {
                String source;
                try {
                  source = Files.readString(path);
                } catch (IOException e) {
                  throw new IllegalStateException(e);
                }
                assertThat(EXTERNAL_CLIENTS.stream().filter(source::contains))
                    .as("external client used in %s", path)
                    .isEmpty();
              });
    }
  }
}
