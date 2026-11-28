package com.matchmap.api.account;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AccountRoles {

  private final UserAccountRepository accounts;

  AccountRoles(UserAccountRepository accounts) {
    this.accounts = accounts;
  }

  public boolean isAdmin(String accountId) {
    return accounts
        .findById(UUID.fromString(accountId))
        .map(account -> account.getRole() == UserRole.ADMIN)
        .orElse(false);
  }
}
