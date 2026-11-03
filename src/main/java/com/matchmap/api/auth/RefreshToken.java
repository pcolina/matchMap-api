package com.matchmap.api.auth;

import com.matchmap.api.account.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private UserAccount account;

  @Column(name = "session_id", nullable = false)
  private UUID sessionId;

  @Column(name = "token_hash", nullable = false, length = 64)
  private String tokenHash;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  protected RefreshToken() {}

  UserAccount getAccount() {
    return account;
  }

  UUID getSessionId() {
    return sessionId;
  }

  Instant getExpiresAt() {
    return expiresAt;
  }

  void markUsed(Instant now) {
    this.usedAt = now;
  }

  boolean isUsedOrRevoked() {
    return usedAt != null || revokedAt != null;
  }

  void revoke(Instant now) {
    if (revokedAt == null) {
      revokedAt = now;
    }
  }

  RefreshToken(
      UUID id,
      UserAccount account,
      UUID sessionId,
      String tokenHash,
      Instant createdAt,
      Instant expiresAt) {
    this.id = id;
    this.account = account;
    this.sessionId = sessionId;
    this.tokenHash = tokenHash;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }
}
