package com.example.trip_sheet_backend.repositories;

import com.example.trip_sheet_backend.models.PasswordResetToken;
import com.example.trip_sheet_backend.models.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByOtpCodeAndUserAccount(String otpCode, UserAccount userAccount);
    Optional<PasswordResetToken> findByUserAccountIdOrderByCreatedAtDesc(UUID userAccountId);
}
