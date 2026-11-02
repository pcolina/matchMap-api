package com.matchmap.api.auth;

import com.matchmap.api.account.UserAccount;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RefreshTokenService {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final RefreshTokenRepository refreshTokens;
  private final Duration ttl;

  RefreshTokenService(RefreshTokenRepository refreshTokens, AuthProperties properties) {
    this.refreshTokens = refreshTokens;
    this.ttl = properties.refreshTokenTtl();
  }

  @Transactional
  String issue(UserAccount account, UUID sessionId) {
    var bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    var token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    var now = Instant.now();
    refreshTokens.save(
        new RefreshToken(UUID.randomUUID(), account, sessionId, hash(token), now, now.plus(ttl)));
    return token;
  }

  private static String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes()));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
