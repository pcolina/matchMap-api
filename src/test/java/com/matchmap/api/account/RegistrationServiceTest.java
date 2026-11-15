package com.matchmap.api.account;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.matchmap.api.moderation.UsernameModeration;
import jakarta.validation.Validation;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class RegistrationServiceTest {

  @Test
  void failsWithoutSavingWhenNoFallbackUsernameIsFree() {
    var accounts = mock(UserAccountRepository.class);
    var passwordEncoder = mock(PasswordEncoder.class);
    var usernames = mock(UsernameGenerator.class);
    var moderation = mock(UsernameModeration.class);
    var validator = Validation.buildDefaultValidatorFactory().getValidator();
    when(accounts.existsByEmail(anyString())).thenReturn(false);
    when(usernames.baseFor(anyString())).thenReturn("nazi");
    when(moderation.isBlocked("nazi")).thenReturn(true);
    when(usernames.fallbackUsername(any())).thenReturn(Optional.empty());
    var service =
        new RegistrationService(accounts, passwordEncoder, validator, usernames, moderation);

    assertThatThrownBy(
            () -> service.register(new RegisterInput("nazi@example.com", "Correcthorse1!")))
        .isInstanceOf(IllegalStateException.class);
    verify(accounts, never()).saveAndFlush(any());
  }
}
