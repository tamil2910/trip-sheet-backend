package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.CustomField;

public interface CustomFieldRepository extends BaseRepository<CustomField, UUID> {
  List<CustomField> findByTenant_IdOrderByCreatedAtDesc(UUID tenantId);

  Optional<CustomField> findByIdAndTenant_Id(UUID id, UUID tenantId);

  boolean existsByTenant_IdAndNameIgnoreCaseAndIsDeletedFalse(UUID tenantId, String name);
}
