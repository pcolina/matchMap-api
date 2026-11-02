package com.matchmap.api.auth;

import com.matchmap.api.account.UserAccount;
import com.matchmap.api.account.UserAccountRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

  private final UserAccountRepository accounts;
  private final PasswordEncoder passwordEncoder;
  private final AccessTokenIssuer accessTokens;
  private final RefreshTokenService refreshTokens;

  AuthController(
      UserAccountRepository accounts,
      PasswordEncoder passwordEncoder,
      AccessTokenIssuer accessTokens,
      RefreshTokenService refreshTokens) {
    this.accounts = accounts;
    this.passwordEncoder = passwordEncoder;
    this.accessTokens = accessTokens;
    this.refreshTokens = refreshTokens;
  }

  @PostMapping("/login")
  public TokenResponse login(@RequestBody LoginRequest request) {
    var account =
        accounts
            .findByEmail(request.email().trim().toLowerCase(Locale.ROOT))
            .filter(candidate -> passwordMatches(candidate, request.password()))
            .orElseThrow(AuthException::invalidCredentials);
    var accessToken = accessTokens.issue(account.getId());
    var refreshToken = refreshTokens.issue(account, UUID.randomUUID());
    return new TokenResponse("Bearer", accessToken, accessTokens.ttl().getSeconds(), refreshToken);
  }

  private boolean passwordMatches(UserAccount account, String password) {
    return account.getPasswordHash() != null
        && passwordEncoder.matches(password, account.getPasswordHash());
  }

  record LoginRequest(String email, String password) {}

  record TokenResponse(String tokenType, String accessToken, long expiresIn, String refreshToken) {}
}
