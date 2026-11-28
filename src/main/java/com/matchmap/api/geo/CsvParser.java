package com.matchmap.api.geo;

import java.util.ArrayList;
import java.util.List;

final class CsvParser {

  record Row(int line, List<String> fields) {}

  private CsvParser() {}

  static List<Row> parse(String text) {
    List<Row> rows = new ArrayList<>();
    String[] lines = text.split("\n", -1);
    for (int i = 0; i < lines.length; i++) {
      String line =
          lines[i].endsWith("\r") ? lines[i].substring(0, lines[i].length() - 1) : lines[i];
      if (line.isBlank()) {
        continue;
      }
      rows.add(new Row(i + 1, splitLine(line)));
    }
    return rows;
  }

  private static List<String> splitLine(String line) {
    List<String> fields = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean quoted = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (quoted) {
        if (c != '"') {
          current.append(c);
        } else if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
          current.append('"');
          i++;
        } else {
          quoted = false;
        }
      } else if (c == '"') {
        quoted = true;
      } else if (c == ',') {
        fields.add(current.toString());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    if (quoted) {
      return null;
    }
    fields.add(current.toString());
    return fields;
  }
}
