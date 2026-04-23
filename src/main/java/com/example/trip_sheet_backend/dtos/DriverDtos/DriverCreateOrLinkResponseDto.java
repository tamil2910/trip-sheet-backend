package com.example.trip_sheet_backend.dtos.DriverDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.Driver;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverCreateOrLinkResponseDto {

  private String action;
  private String uniqueCode;
  private Boolean driverExists;
  private Boolean linkedToTenant;
  private UUID tenantId;
  private UUID mappingId;
  private Driver driver;
}
