package com.matchmap.api.account;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleAccountService {

  private static final int MAX_DISPLAY_NAME_LENGTH = 50;

  private final UserAccountRepository accounts;
  private final UsernameGenerator usernames;

  GoogleAccountService(UserAccountRepository accounts, UsernameGenerator usernames) {
    this.accounts = accounts;
    this.usernames = usernames;
  }

  @Transactional
  public UserAccount resolve(String subject, String email, String name) {
    return accounts
        .findByGoogleSubject(subject)
        .orElseGet(() -> create(subject, email.trim().toLowerCase(Locale.ROOT), name));
  }

  private UserAccount create(String subject, String email, String name) {
    var account =
        new UserAccount(
            UUID.randomUUID(),
            email,
            usernames.generate(email, accounts::existsByUsername),
            displayName(name, email),
            null,
            subject,
            Instant.now());
    return accounts.saveAndFlush(account);
  }

  private static String displayName(String name, String email) {
    var trimmed = name == null ? "" : name.trim();
    var value = trimmed.isEmpty() ? email.substring(0, email.indexOf('@')) : trimmed;
    return value.length() > MAX_DISPLAY_NAME_LENGTH
        ? value.substring(0, MAX_DISPLAY_NAME_LENGTH)
        : value;
  }
}
