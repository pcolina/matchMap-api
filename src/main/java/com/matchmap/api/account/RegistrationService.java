package com.matchmap.api.account;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RegistrationService {

  private static final List<String> FIELD_ORDER =
      List.of("email", "username", "displayName", "password");

  private final UserAccountRepository accounts;
  private final PasswordEncoder passwordEncoder;
  private final Validator validator;

  RegistrationService(
      UserAccountRepository accounts, PasswordEncoder passwordEncoder, Validator validator) {
    this.accounts = accounts;
    this.passwordEncoder = passwordEncoder;
    this.validator = validator;
  }

  @Transactional
  UserAccount register(RegisterInput input) {
    var registration = Registration.from(input);
    validate(registration);
    if (accounts.existsByEmail(registration.email())) {
      throw AccountException.emailTaken();
    }
    if (accounts.existsByUsername(registration.username())) {
      throw AccountException.usernameTaken();
    }
    var account =
        new UserAccount(
            UUID.randomUUID(),
            registration.email(),
            registration.username(),
            registration.displayName(),
            passwordEncoder.encode(registration.password()),
            Instant.now());
    try {
      return accounts.saveAndFlush(account);
    } catch (DataIntegrityViolationException e) {
      if (accounts.existsByUsername(registration.username())) {
        throw AccountException.usernameTaken();
      }
      throw AccountException.emailTaken();
    }
  }

  private void validate(Registration registration) {
    validator.validate(registration).stream()
        .map(ConstraintViolation::getPropertyPath)
        .map(Object::toString)
        .min(Comparator.comparingInt(FIELD_ORDER::indexOf))
        .ifPresent(
            field -> {
              throw AccountException.validationFailed(field);
            });
  }

  record Registration(
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Pattern(regexp = "[a-z0-9_]{3,30}") String username,
      @NotBlank @Size(max = 50) String displayName,
      @NotNull @Size(min = 12, max = 128) String password) {

    static Registration from(RegisterInput input) {
      return new Registration(
          normalizeEmail(input.email()),
          normalizeUsername(input.username()),
          normalizeDisplayName(input.displayName()),
          input.password());
    }

    private static String normalizeDisplayName(String displayName) {
      return displayName == null ? null : displayName.trim();
    }

    private static String normalizeEmail(String email) {
      return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeUsername(String username) {
      return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }
  }
}
