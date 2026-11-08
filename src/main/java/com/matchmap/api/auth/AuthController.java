package com.matchmap.api.auth;

import com.matchmap.api.account.GoogleAccountService;
import com.matchmap.api.account.UserAccount;
import com.matchmap.api.account.UserAccountRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nimbusds.jose.KeySourceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

  private final UserAccountRepository accounts;
  private final PasswordEncoder passwordEncoder;
  private final AccessTokenIssuer accessTokens;
  private final RefreshTokenService refreshTokens;
  private final LoginThrottle loginThrottle;
  private final GoogleIdTokenVerifier googleIdTokens;
  private final GoogleAccountService googleAccounts;
  private final Validator validator;
  private final AuthProperties authProperties;
  private final String refreshCookieSameSite;

  AuthController(
      UserAccountRepository accounts,
      PasswordEncoder passwordEncoder,
      AccessTokenIssuer accessTokens,
      RefreshTokenService refreshTokens,
      LoginThrottle loginThrottle,
      GoogleIdTokenVerifier googleIdTokens,
      GoogleAccountService googleAccounts,
      Validator validator,
      AuthProperties authProperties,
      @Value("${matchmap.auth.refresh-cookie.same-site}") String refreshCookieSameSite) {
    this.accounts = accounts;
    this.passwordEncoder = passwordEncoder;
    this.accessTokens = accessTokens;
    this.refreshTokens = refreshTokens;
    this.loginThrottle = loginThrottle;
    this.googleIdTokens = googleIdTokens;
    this.googleAccounts = googleAccounts;
    this.validator = validator;
    this.authProperties = authProperties;
    this.refreshCookieSameSite = refreshCookieSameSite;
  }

  @PostMapping("/google")
  public TokenResponse google(
      @RequestBody GoogleRequest request,
      @RequestHeader(value = "X-Client", required = false) String client,
      HttpServletResponse httpResponse) {
    var clientType = ClientType.from(client);
    GoogleIdentity identity;
    try {
      identity = googleIdTokens.verify(request.idToken());
    } catch (JwtException e) {
      if (e.getCause() instanceof KeySourceException) {
        throw AuthException.googleUnavailable();
      }
      throw AuthException.invalidGoogleToken();
    }
    if (!identity.emailVerified()) {
      throw AuthException.googleEmailNotVerified();
    }
    var account =
        googleAccounts
            .resolve(identity.subject(), identity.email(), identity.name())
            .orElseThrow(AuthException::invalidGoogleToken);
    return signedInResponse(account, clientType, httpResponse);
  }

  @PostMapping("/login")
  public TokenResponse login(
      @RequestBody LoginRequest request,
      @RequestHeader(value = "X-Client", required = false) String client,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    var clientType = ClientType.from(client);
    var email = request.email() == null ? null : request.email().trim().toLowerCase(Locale.ROOT);
    var password = request.password() == null ? null : request.password().strip();
    validateLoginLimits(email, password);
    var throttleKey = email + "|" + httpRequest.getRemoteAddr();
    var now = Instant.now();
    var retryAfter = loginThrottle.retryAfter(throttleKey, now);
    if (retryAfter.isPresent()) {
      throw AuthException.tooManyAttempts(retryAfter.get());
    }
    var found = accounts.findByEmail(email).filter(c -> passwordMatches(c, password));
    if (found.isEmpty()) {
      loginThrottle.recordFailure(throttleKey, now);
      throw AuthException.invalidCredentials();
    }
    loginThrottle.reset(throttleKey);
    return signedInResponse(found.get(), clientType, httpResponse);
  }

  @PostMapping("/refresh")
  public TokenResponse refresh(
      @RequestBody RefreshRequest request,
      @RequestHeader(value = "X-Client", required = false) String client) {
    ClientType.from(client);
    var rotation = refreshTokens.rotate(request.refreshToken());
    return tokenResponse(rotation.account(), rotation.refreshToken());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(
      @RequestBody RefreshRequest request,
      @RequestHeader(value = "X-Client", required = false) String client) {
    ClientType.from(client);
    refreshTokens.logout(request.refreshToken());
  }

  private TokenResponse tokenResponse(UserAccount account, String refreshToken) {
    return new TokenResponse(
        "Bearer",
        accessTokens.issue(account.getId()),
        accessTokens.ttl().getSeconds(),
        refreshToken);
  }

  private TokenResponse signedInResponse(
      UserAccount account, ClientType clientType, HttpServletResponse httpResponse) {
    var refreshToken = refreshTokens.issue(account, UUID.randomUUID());
    if (clientType == ClientType.WEB) {
      httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(refreshToken).toString());
      return tokenResponse(account, null);
    }
    return tokenResponse(account, refreshToken);
  }

  private ResponseCookie refreshCookie(String refreshToken) {
    return ResponseCookie.from("refresh_token", refreshToken)
        .httpOnly(true)
        .secure(true)
        .path("/api/auth")
        .maxAge(authProperties.refreshTokenTtl())
        .sameSite(refreshCookieSameSite)
        .build();
  }

  private void validateLoginLimits(String email, String password) {
    var violations = validator.validate(new LoginLimits(email, password));
    var fields = violations.stream().map(v -> v.getPropertyPath().toString()).toList();
    if (fields.contains("email")) {
      throw AuthException.invalidEmail();
    }
    if (fields.contains("password")) {
      throw AuthException.invalidPassword();
    }
  }

  private boolean passwordMatches(UserAccount account, String password) {
    return account.getPasswordHash() != null
        && passwordEncoder.matches(password, account.getPasswordHash());
  }

  record LoginRequest(String email, String password) {}

  record RefreshRequest(String refreshToken) {}

  record GoogleRequest(String idToken) {}

  record TokenResponse(
      String tokenType,
      String accessToken,
      long expiresIn,
      @JsonInclude(JsonInclude.Include.NON_NULL) String refreshToken) {}
}
