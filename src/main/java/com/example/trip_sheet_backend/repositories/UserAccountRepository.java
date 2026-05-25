package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.UserAccount;

public interface UserAccountRepository extends BaseRepository<UserAccount, UUID> {
  Optional<UserAccount> findByEmail(String email);
  List<UserAccount> findAllByEmailOrderByCreatedAtDesc(String email);
  Optional<UserAccount> findByPhone(String phone);
  Optional<UserAccount> findByUsername(String username);

  boolean existsByEmail(String email);
  boolean existsByPhone(String phone);

}
