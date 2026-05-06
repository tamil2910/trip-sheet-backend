package com.example.trip_sheet_backend.services.DriverService;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.trip_sheet_backend.common.services.GlobalBaseService;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverCreateOrLinkRequestDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverCreateOrLinkResponseDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverCodeLookupResponseDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverTenantResponseDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverUpdateRequestDto;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.Tenant;

public interface DriverService extends GlobalBaseService<Driver, UUID> {

  DriverCreateOrLinkResponseDto createOrLinkDriver(
      DriverCreateOrLinkRequestDto body,
      Tenant tokenTenant,
      UUID createdBy
  );

  DriverCodeLookupResponseDto getDriverByUniqueCode(Tenant tokenTenant, String uniqueCode);

  List<DriverTenantResponseDto> getDriversByTenant(Tenant tokenTenant);

    Page<DriverTenantResponseDto> searchDriversByTenant(
            Tenant tokenTenant,
            String fullName,
            String phone,
            String email,
            Pageable pageable
    );

  DriverTenantResponseDto getDriverByTenant(Tenant tokenTenant, UUID driverId);

    DriverTenantResponseDto linkDriverToCurrentTenant(
      Tenant tokenTenant,
      UUID driverId,
      UUID createdBy
    );

  DriverTenantResponseDto updateDriverByTenant(
      Tenant tokenTenant,
      UUID driverId,
      DriverUpdateRequestDto body,
      UUID updatedBy
  );

  DriverTenantResponseDto setDriverActiveForTenant(
      Tenant tokenTenant,
      UUID driverId,
      boolean active,
      UUID updatedBy
  );
}
