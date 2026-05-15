package com.example.trip_sheet_backend.dtos.TenantDtos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenantCodeRequestDto {

  @NotBlank(message = "tenantUniqueCode is required")
  private String tenantUniqueCode;

  private List<UUID> taxIds = new ArrayList<>();
}
