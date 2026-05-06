package com.example.trip_sheet_backend.dtos.DriverDtos;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverTenantLinkRequestDto {

  @NotNull(message = "Driver id is required")
  private UUID driverId;
}