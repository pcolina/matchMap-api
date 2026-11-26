package com.matchmap.api.account;

import java.security.Principal;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
class AccountGraphqlController {

  private final RegistrationService registrationService;
  private final UserAccountRepository accounts;
  private final FirstVisitService firstVisitService;

  AccountGraphqlController(
      RegistrationService registrationService,
      UserAccountRepository accounts,
      FirstVisitService firstVisitService) {
    this.registrationService = registrationService;
    this.accounts = accounts;
    this.firstVisitService = firstVisitService;
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

  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  UserAccount completeFirstVisit(Principal principal) {
    return firstVisitService.complete(UUID.fromString(principal.getName()));
  }

  @SchemaMapping(typeName = "User", field = "isFirstVisit")
  boolean isFirstVisit(UserAccount account) {
    return account.isFirstVisit();
  }
}
