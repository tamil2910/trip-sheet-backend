package com.example.trip_sheet_backend.services.CustomTaxService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.dtos.CustomTaxDtos.CustomTaxRequestDto;
import com.example.trip_sheet_backend.models.CustomTax;
import com.example.trip_sheet_backend.models.Tenant;

public interface CustomTaxService {
  CustomTax create(CustomTaxRequestDto body, Tenant tenant, UUID createdBy);

  List<CustomTax> getAll(Tenant tenant);

  CustomTax getById(UUID id, Tenant tenant);

  CustomTax update(UUID id, CustomTaxRequestDto body, Tenant tenant, UUID updatedBy);

  void delete(UUID id, Tenant tenant, UUID deletedBy);
}
