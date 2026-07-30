package com.example.trip_sheet_backend.dtos.VendorOrganisationTaxDtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.trip_sheet_backend.models.Tax.TaxType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorOrganisationTaxCreateRequestDto {

  @NotNull(message = "taxPercentage is required")
  @DecimalMin(value = "0.00", inclusive = false, message = "taxPercentage must be greater than 0")
  private BigDecimal taxPercentage;

  @NotNull(message = "taxType is required")
  private TaxType taxType;

  @NotNull(message = "vendorOrganisationId is required")
  private UUID vendorOrganisationId;
}
