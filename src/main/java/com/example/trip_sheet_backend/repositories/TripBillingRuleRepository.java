package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.TripBillingRule;

public interface TripBillingRuleRepository extends BaseRepository<TripBillingRule, UUID> {
  Optional<TripBillingRule> findFirstByTenant_IdAndActiveTrueAndIsDeletedFalseOrderByUpdatedAtDesc(UUID tenantId);

  List<TripBillingRule> findByTenant_IdAndIsDeletedFalseOrderByUpdatedAtDesc(UUID tenantId);

  Optional<TripBillingRule> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);
}
