package com.example.trip_sheet_backend.services.TaxService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.common.services.BaseService;
import com.example.trip_sheet_backend.dtos.TaxDtos.CreateTaxRequestDto;
import com.example.trip_sheet_backend.models.Tax;
import com.example.trip_sheet_backend.models.Tenant;

public interface TaxService extends BaseService<Tax, UUID> {
  Tax createTax(CreateTaxRequestDto body, Tenant tokenTenant, UUID createdBy);

  List<Tax> getTaxesByTenant(Tenant tokenTenant);

  Tax updateTax(UUID taxId, CreateTaxRequestDto body, Tenant tokenTenant, UUID updatedBy);

  void deleteTax(UUID taxId, Tenant tokenTenant);
}
