package com.matchmap.api.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

  @Column(name = "first_visit", nullable = false)
  private boolean firstVisit;

  @Column(name = "first_name", length = 100)
  private String firstName;

  @Column(name = "last_name", length = 100)
  private String lastName;

  @Column(name = "nationality", length = 2)
  private String nationality;

  @Column(name = "birth_country", length = 2)
  private String birthCountry;

  @Enumerated(EnumType.STRING)
  @Column(name = "subscription", nullable = false, length = 20)
  private SubscriptionPlan subscription;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 10)
  private UserRole role;

  protected UserAccount() {}

  UserAccount(UUID id, String email, String username, String passwordHash, Instant createdAt) {
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
    this.firstVisit = true;
    this.subscription = SubscriptionPlan.FREE;
    this.role = UserRole.USER;
  }

  public UserRole getRole() {
    return role;
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

  void completeFirstVisit() {
    this.firstVisit = false;
  }

  public boolean isFirstVisit() {
    return firstVisit;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getNationality() {
    return nationality;
  }

  public String getBirthCountry() {
    return birthCountry;
  }

  public SubscriptionPlan getSubscription() {
    return subscription;
  }
}
