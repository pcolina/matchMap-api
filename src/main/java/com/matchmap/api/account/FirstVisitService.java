package com.matchmap.api.account;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class FirstVisitService {

  private final UserAccountRepository accounts;

  FirstVisitService(UserAccountRepository accounts) {
    this.accounts = accounts;
  }

  @Transactional
  UserAccount complete(UUID accountId) {
    UserAccount account = accounts.findById(accountId).orElseThrow();
    account.completeFirstVisit();
    return account;
  }
}
