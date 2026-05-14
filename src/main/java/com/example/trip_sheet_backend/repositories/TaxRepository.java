package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Tax;

public interface TaxRepository extends BaseRepository<Tax, UUID> {
  Optional<Tax> findByTenant_IdAndTaxNameIgnoreCase(UUID tenantId, String taxName);

  Optional<Tax> findByIdAndTenant_Id(UUID id, UUID tenantId);

  List<Tax> findByTenant_IdOrderByTaxPercentageAscTaxTypeAsc(UUID tenantId);
}
