package com.example.trip_sheet_backend.services.TenantService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.common.services.GlobalBaseService;
import com.example.trip_sheet_backend.dtos.TenantDtos.TenantLinkResponseDto;
import com.example.trip_sheet_backend.models.Tenant;

public interface TenantService extends GlobalBaseService<Tenant, UUID> {
  Tenant findByIdResource(UUID id);

  Tenant findByUniqueCode(String tenantUniqueCode);

  TenantLinkResponseDto linkExistingTenantByUniqueCode(Tenant loggedInTenant, String tenantUniqueCode, UUID createdBy, List<UUID> taxIds);
}
