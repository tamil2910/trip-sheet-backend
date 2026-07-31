package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.OrganisationSettings;

public interface OrganisationSettingsRepository extends BaseRepository<OrganisationSettings, UUID> {
  List<OrganisationSettings> findByTenant_IdAndIsDeletedFalseOrderByUpdatedAtDesc(UUID tenantId);

  Optional<OrganisationSettings> findFirstByTenant_IdAndIsDeletedFalseOrderByUpdatedAtDesc(UUID tenantId);

  Optional<OrganisationSettings> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);

  boolean existsByTenant_IdAndIsDeletedFalse(UUID tenantId);
}
