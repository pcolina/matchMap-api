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

  private static final List<String> FIELD_ORDER = List.of("email", "password");

  private final UserAccountRepository accounts;
  private final PasswordEncoder passwordEncoder;
  private final Validator validator;
  private final UsernameGenerator usernames;

  RegistrationService(
      UserAccountRepository accounts,
      PasswordEncoder passwordEncoder,
      Validator validator,
      UsernameGenerator usernames) {
    this.accounts = accounts;
    this.passwordEncoder = passwordEncoder;
    this.validator = validator;
    this.usernames = usernames;
  }

  @Transactional
  UserAccount register(RegisterInput input) {
    var registration = Registration.from(input);
    validate(registration);
    if (accounts.existsByEmail(registration.email())) {
      throw AccountException.emailTaken();
    }
    var username = usernames.generate(registration.email(), accounts::existsByUsername);
    var account =
        new UserAccount(
            UUID.randomUUID(),
            registration.email(),
            username,
            passwordEncoder.encode(registration.password()),
            Instant.now());
    try {
      return accounts.saveAndFlush(account);
    } catch (DataIntegrityViolationException e) {
      if (accounts.existsByUsername(username)) {
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
              throw "password".equals(field)
                  ? AccountException.passwordInvalid()
                  : AccountException.validationFailed(field);
            });
  }

  record Registration(
      @NotBlank @Email @Size(max = 254) String email,
      @NotNull
          @Size(min = 6, max = 20)
          @Pattern(
              regexp =
                  "(?U)(?=.*[a-z])(?=.*[A-Z])(?=.*[!\\\"#$%&'()*+,\\-./:;<=>?@\\[\\\\\\]^_`{|}~])\\S*")
          String password) {

    static Registration from(RegisterInput input) {
      return new Registration(normalizeEmail(input.email()), input.password());
    }

    private static String normalizeEmail(String email) {
      return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
  }
}
