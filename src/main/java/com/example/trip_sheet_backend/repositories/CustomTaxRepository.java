package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.CustomTax;

public interface CustomTaxRepository extends BaseRepository<CustomTax, UUID> {
  @EntityGraph(attributePaths = { "tenant", "tax" })
  Optional<CustomTax> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);

  Optional<CustomTax> findByTenant_IdAndTax_IdAndIsDeletedFalse(UUID tenantId, UUID taxId);

  @EntityGraph(attributePaths = { "tenant", "tax" })
  List<CustomTax> findByTenant_IdAndIsDeletedFalseOrderByUpdatedAtDesc(UUID tenantId);
}
