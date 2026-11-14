package com.example.trip_sheet_backend.repositories;

import java.util.UUID;
import java.util.Optional;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.DispatchCenter;

public interface DispatchCenterRepository extends BaseRepository<DispatchCenter, UUID> {
  Optional<DispatchCenter> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);
}
