package com.example.trip_sheet_backend.repositories;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Shift;

public interface ShiftRepository extends BaseRepository<Shift, UUID> {
    Optional<Shift> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);

    Page<Shift> findByTenant_IdAndIsDeletedFalse(UUID tenantId, Pageable pageable);

    Page<Shift> findByTenant_IdAndNameContainingIgnoreCaseAndIsDeletedFalse(
            UUID tenantId, String name, Pageable pageable);
}
