package com.matchmap.api.account;

import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RegistrationService {

  private final UserAccountRepository accounts;
  private final PasswordEncoder passwordEncoder;

  RegistrationService(UserAccountRepository accounts, PasswordEncoder passwordEncoder) {
    this.accounts = accounts;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  UserAccount register(RegisterInput input) {
    var account =
        new UserAccount(
            UUID.randomUUID(),
            input.email(),
            input.username(),
            input.displayName(),
            passwordEncoder.encode(input.password()),
            Instant.now());
    return accounts.save(account);
  }
}
