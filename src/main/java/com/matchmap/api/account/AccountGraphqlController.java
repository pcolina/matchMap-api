package com.matchmap.api.account;

import java.security.Principal;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
class AccountGraphqlController {

  private final RegistrationService registrationService;
  private final UserAccountRepository accounts;

  AccountGraphqlController(
      RegistrationService registrationService, UserAccountRepository accounts) {
    this.registrationService = registrationService;
    this.accounts = accounts;
  }

  @MutationMapping
  UserAccount register(@Argument RegisterInput input) {
    return registrationService.register(input);
  }

  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  public UserAccount me(Principal principal) {
    return accounts.findById(UUID.fromString(principal.getName())).orElseThrow();
  }
}
