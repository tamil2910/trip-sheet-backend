package com.example.trip_sheet_backend.services.TaxService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.common.services.GlobalBaseService;
import com.example.trip_sheet_backend.dtos.TaxDtos.CreateTaxRequestDto;
import com.example.trip_sheet_backend.models.Tax;

public interface TaxService extends GlobalBaseService<Tax, UUID> {
  Tax createTax(CreateTaxRequestDto body, UUID createdBy);

  List<Tax> getTaxes();

  Tax getTaxById(UUID taxId);

  Tax updateTax(UUID taxId, CreateTaxRequestDto body, UUID updatedBy);

  void deleteTax(UUID taxId, UUID deletedBy);
}
