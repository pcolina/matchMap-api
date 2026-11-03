package com.matchmap.api.account;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

  Optional<UserAccount> findByEmail(String email);

  Optional<UserAccount> findByGoogleSubject(String googleSubject);

  boolean existsByEmail(String email);

  boolean existsByUsername(String username);
}
