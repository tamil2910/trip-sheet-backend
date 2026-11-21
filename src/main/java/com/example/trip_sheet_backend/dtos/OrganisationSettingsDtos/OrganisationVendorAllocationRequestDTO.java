package com.example.trip_sheet_backend.dtos.OrganisationSettingsDtos;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganisationVendorAllocationRequestDTO {
  @NotNull
  private UUID vendorOrganisationId;

  @NotNull
  @DecimalMin("0.00")
  @DecimalMax("100.00")
  @Digits(integer = 3, fraction = 2)
  private BigDecimal allocationPercentage;
}
