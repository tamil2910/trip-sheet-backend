package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.BankAccount;

public interface BankAccountRepository extends BaseRepository<BankAccount, UUID> {
    Optional<BankAccount> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);
}
