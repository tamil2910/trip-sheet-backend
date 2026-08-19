package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.InvoiceNumberRule;

public interface InvoiceNumberRuleRepository extends BaseRepository<InvoiceNumberRule, UUID> {
  List<InvoiceNumberRule> findByTenant_IdAndIsDeletedFalseOrderByUpdatedAtDesc(UUID tenantId);

  Optional<InvoiceNumberRule> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);

  Optional<InvoiceNumberRule> findByTenant_IdAndIsDefaultTrueAndIsDeletedFalse(UUID tenantId);

  long countByTenant_IdAndIsDeletedFalse(UUID tenantId);
}
