package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.trip_sheet_backend.models.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
  Optional<UserAccount> findByEmail(String email);
  Optional<UserAccount> findByPhone(Long phone);
  Optional<UserAccount> findByUsername(String username);
}
