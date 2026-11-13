package com.example.trip_sheet_backend.repositories;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Shift;

public interface ShiftRepository extends BaseRepository<Shift, UUID> {
    Optional<Shift> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);

    Optional<Shift> findByIdAndOrganisation_IdAndIsDeletedFalse(UUID id, UUID organisationId);

    Page<Shift> findByTenant_IdAndIsDeletedFalse(UUID tenantId, Pageable pageable);

    Page<Shift> findByTenant_IdAndNameContainingIgnoreCaseAndIsDeletedFalse(
            UUID tenantId, String name, Pageable pageable);

    Page<Shift> findByOrganisation_IdAndIsDeletedFalse(UUID organisationId, Pageable pageable);

    Page<Shift> findByOrganisation_IdAndNameContainingIgnoreCaseAndIsDeletedFalse(
            UUID organisationId, String name, Pageable pageable);

    Page<Shift> findByTenant_IdAndOrganisation_IdAndIsDeletedFalse(
            UUID tenantId, UUID organisationId, Pageable pageable);

    Page<Shift> findByTenant_IdAndOrganisation_IdAndNameContainingIgnoreCaseAndIsDeletedFalse(
            UUID tenantId, UUID organisationId, String name, Pageable pageable);
}
