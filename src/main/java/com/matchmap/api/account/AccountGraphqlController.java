package com.matchmap.api.account;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
class AccountGraphqlController {

  private final RegistrationService registrationService;

  AccountGraphqlController(RegistrationService registrationService) {
    this.registrationService = registrationService;
  }

  @MutationMapping
  UserAccount register(@Argument RegisterInput input) {
    return registrationService.register(input);
  }
}
