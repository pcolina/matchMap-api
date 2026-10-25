package com.matchmap.api.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_account")
public class UserAccount {

  @Id private UUID id;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(nullable = false, length = 30)
  private String username;

  @Column(name = "display_name", nullable = false, length = 50)
  private String displayName;

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "google_subject")
  private String googleSubject;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected UserAccount() {}

  UserAccount(
      UUID id,
      String email,
      String username,
      String displayName,
      String passwordHash,
      Instant createdAt) {
    this.id = id;
    this.email = email;
    this.username = username;
    this.displayName = displayName;
    this.passwordHash = passwordHash;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getUsername() {
    return username;
  }

  public String getDisplayName() {
    return displayName;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
