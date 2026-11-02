package com.matchmap.api.auth;

import com.matchmap.api.account.UserAccount;
import com.matchmap.api.account.UserAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

  AuthController(
      UserAccountRepository accounts,
      PasswordEncoder passwordEncoder,
      AccessTokenIssuer accessTokens,
      RefreshTokenService refreshTokens,
      LoginThrottle loginThrottle) {
    this.accounts = accounts;
    this.passwordEncoder = passwordEncoder;
    this.accessTokens = accessTokens;
    this.refreshTokens = refreshTokens;
    this.loginThrottle = loginThrottle;
  }

  @PostMapping("/login")
  public TokenResponse login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    var email = request.email().trim().toLowerCase(Locale.ROOT);
    var throttleKey = email + "|" + httpRequest.getRemoteAddr();
    var now = Instant.now();
    var retryAfter = loginThrottle.retryAfter(throttleKey, now);
    if (retryAfter.isPresent()) {
      throw AuthException.tooManyAttempts(retryAfter.get());
    }
    var found = accounts.findByEmail(email).filter(c -> passwordMatches(c, request.password()));
    if (found.isEmpty()) {
      loginThrottle.recordFailure(throttleKey, now);
      throw AuthException.invalidCredentials();
    }
    loginThrottle.reset(throttleKey);
    var account = found.get();
    return tokenResponse(account, refreshTokens.issue(account, UUID.randomUUID()));
  }

  @PostMapping("/refresh")
  public TokenResponse refresh(@RequestBody RefreshRequest request) {
    var rotation = refreshTokens.rotate(request.refreshToken());
    return tokenResponse(rotation.account(), rotation.refreshToken());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@RequestBody RefreshRequest request) {
    refreshTokens.logout(request.refreshToken());
  }

  private TokenResponse tokenResponse(UserAccount account, String refreshToken) {
    return new TokenResponse(
        "Bearer",
        accessTokens.issue(account.getId()),
        accessTokens.ttl().getSeconds(),
        refreshToken);
  }

  private boolean passwordMatches(UserAccount account, String password) {
    return account.getPasswordHash() != null
        && passwordEncoder.matches(password, account.getPasswordHash());
  }

  record LoginRequest(String email, String password) {}

  record RefreshRequest(String refreshToken) {}

  record TokenResponse(String tokenType, String accessToken, long expiresIn, String refreshToken) {}
}
