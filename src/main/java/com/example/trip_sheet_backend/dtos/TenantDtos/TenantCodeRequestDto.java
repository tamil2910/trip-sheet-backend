package com.example.trip_sheet_backend.dtos.TenantDtos;


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

}
