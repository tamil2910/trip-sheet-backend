package com.example.trip_sheet_backend.dtos.TenantDtos;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

  private List<@NotNull(message = "Tax id cannot be null") UUID> taxIds;

}
