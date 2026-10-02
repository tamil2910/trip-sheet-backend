package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Label;

public interface LabelRepository extends BaseRepository<Label, UUID> {
  List<Label> findByTenant_IdAndIsDeletedFalseOrderByNameAsc(UUID tenantId);

  List<Label> findByTenant_IdAndIsDeletedFalseAndNameContainingIgnoreCaseOrderByNameAsc(UUID tenantId, String name);

  Optional<Label> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);

  boolean existsByTenant_IdAndNameIgnoreCaseAndIsDeletedFalse(UUID tenantId, String name);

  boolean existsByTenant_IdAndNameIgnoreCaseAndIsDeletedFalseAndIdNot(UUID tenantId, String name, UUID excludedId);
}