package com.example.trip_sheet_backend.services.DriverService;

import java.util.UUID;

import com.example.trip_sheet_backend.common.services.GlobalBaseService;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverCreateOrLinkRequestDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverCreateOrLinkResponseDto;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.Tenant;

public interface DriverService extends GlobalBaseService<Driver, UUID> {

  DriverCreateOrLinkResponseDto createOrLinkDriver(
      DriverCreateOrLinkRequestDto body,
      Tenant tokenTenant,
      UUID createdBy
  );
}
