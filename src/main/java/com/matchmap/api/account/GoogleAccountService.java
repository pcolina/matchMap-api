package com.matchmap.api.account;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleAccountService {

  private final UserAccountRepository accounts;
  private final UsernameGenerator usernames;

  GoogleAccountService(UserAccountRepository accounts, UsernameGenerator usernames) {
    this.accounts = accounts;
    this.usernames = usernames;
  }

  @Transactional
  public Optional<UserAccount> resolve(String subject, String email, String name) {
    var normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
    var linked = accounts.findByGoogleSubject(subject);
    if (linked.isPresent()) {
      return linked;
    }
    var existing = accounts.findByEmail(normalizedEmail);
    if (existing.isPresent()) {
      return link(existing.get(), subject);
    }
    return Optional.of(create(subject, normalizedEmail, name));
  }

  private Optional<UserAccount> link(UserAccount account, String subject) {
    if (account.getGoogleSubject() != null) {
      return Optional.empty();
    }
    account.linkGoogleSubject(subject);
    return Optional.of(accounts.saveAndFlush(account));
  }

  private static String sourceName(String name, String email) {
    var trimmed = name == null ? "" : name.trim();
    return trimmed.isEmpty() ? email.substring(0, email.indexOf('@')) : trimmed;
  }

  private UserAccount create(String subject, String email, String name) {
    var account =
        new UserAccount(
            UUID.randomUUID(),
            email,
            usernames.generateFromName(sourceName(name, email), accounts::existsByUsername),
            null,
            subject,
            Instant.now());
    return accounts.saveAndFlush(account);
  }
}
