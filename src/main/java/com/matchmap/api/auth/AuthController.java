package com.matchmap.api.auth;

import com.matchmap.api.account.UserAccountRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

  private final UserAccountRepository accounts;
  private final AccessTokenIssuer accessTokens;
  private final RefreshTokenService refreshTokens;

  AuthController(
      UserAccountRepository accounts,
      AccessTokenIssuer accessTokens,
      RefreshTokenService refreshTokens) {
    this.accounts = accounts;
    this.accessTokens = accessTokens;
    this.refreshTokens = refreshTokens;
  }

  @PostMapping("/login")
  public TokenResponse login(@RequestBody LoginRequest request) {
    var account =
        accounts.findByEmail(request.email().trim().toLowerCase(Locale.ROOT)).orElseThrow();
    var accessToken = accessTokens.issue(account.getId());
    var refreshToken = refreshTokens.issue(account, UUID.randomUUID());
    return new TokenResponse("Bearer", accessToken, accessTokens.ttl().getSeconds(), refreshToken);
  }

  record LoginRequest(String email, String password) {}

  record TokenResponse(String tokenType, String accessToken, long expiresIn, String refreshToken) {}
}
