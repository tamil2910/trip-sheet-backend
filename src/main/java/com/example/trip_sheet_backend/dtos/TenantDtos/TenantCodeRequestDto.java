package com.example.trip_sheet_backend.dtos.TenantDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantCodeRequestDto {

  @NotBlank(message = "tenantUniqueCode is required")
  private String tenantUniqueCode;
}
