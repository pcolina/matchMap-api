package com.matchmap.api.moderation;

import java.util.List;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
class ProhibitedWordGraphqlController {

  private final ProhibitedWordAdminService admin;

  ProhibitedWordGraphqlController(ProhibitedWordAdminService admin) {
    this.admin = admin;
  }

  @QueryMapping
  List<ProhibitedWord> prohibitedWords() {
    return admin.list();
  }

  @MutationMapping
  ProhibitedWord createProhibitedWord(
      @Argument String word, @Argument String category, @Argument String language) {
    return admin.create(word, category, language);
  }

  @MutationMapping
  ProhibitedWord setProhibitedWordActive(@Argument String id, @Argument boolean active) {
    return admin.setActive(parseId(id), active);
  }

  private static UUID parseId(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      throw ModerationException.invalid("id", "Invalid id");
    }
  }
}
