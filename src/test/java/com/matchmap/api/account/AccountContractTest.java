package com.matchmap.api.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class AccountContractTest {

  private static final Pattern REGISTER_DESCRIPTION =
      Pattern.compile("\"\"\"(.*?)\"\"\"\\s*register\\(", Pattern.DOTALL);

  @Test
  void registerDescriptionNamesTheStableCodes() throws IOException {
    String contract = Files.readString(Path.of("contracts/graphql/account.graphqls"));

    Matcher matcher = REGISTER_DESCRIPTION.matcher(contract);

    assertThat(matcher.find()).isTrue();
    assertThat(matcher.group(1))
        .contains("EMAIL_TAKEN")
        .contains("USERNAME_TAKEN")
        .contains("VALIDATION_FAILED")
        .contains("extensions.field");
  }

  @Test
  void registerDescriptionDocumentsThePasswordRule() throws IOException {
    String contract = Files.readString(Path.of("contracts/graphql/account.graphqls"));

    Matcher matcher = REGISTER_DESCRIPTION.matcher(contract);

    assertThat(matcher.find()).isTrue();
    assertThat(matcher.group(1))
        .contains("6 to 20 characters")
        .contains("lowercase")
        .contains("uppercase")
        .contains("special character")
        .contains("OWASP")
        .contains("whitespace");
  }
}
