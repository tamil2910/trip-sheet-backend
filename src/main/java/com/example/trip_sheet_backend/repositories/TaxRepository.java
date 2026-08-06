package com.example.trip_sheet_backend.repositories;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Tax;

public interface TaxRepository extends BaseRepository<Tax, UUID> {
  Optional<Tax> findByTaxNameIgnoreCaseAndTaxPercentageAndTaxType(
      String taxName,
      BigDecimal taxPercentage,
      Tax.TaxType taxType);

  Optional<Tax> findById(UUID id);

  java.util.List<Tax> findByIsDeletedFalseOrderByTaxPercentageAscTaxTypeAsc();
}
