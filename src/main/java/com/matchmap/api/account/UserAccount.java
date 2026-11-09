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

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "google_subject")
  private String googleSubject;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected UserAccount() {}

  UserAccount(
      UUID id, String email, String username, String passwordHash, Instant createdAt) {
    this(id, email, username, passwordHash, null, createdAt);
  }

  UserAccount(
      UUID id,
      String email,
      String username,
      String passwordHash,
      String googleSubject,
      Instant createdAt) {
    this.id = id;
    this.email = email;
    this.username = username;
    this.passwordHash = passwordHash;
    this.googleSubject = googleSubject;
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

  public String getPasswordHash() {
    return passwordHash;
  }

  String getGoogleSubject() {
    return googleSubject;
  }

  void linkGoogleSubject(String subject) {
    this.googleSubject = subject;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
