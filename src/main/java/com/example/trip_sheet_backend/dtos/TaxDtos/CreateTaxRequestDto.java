package com.example.trip_sheet_backend.dtos.TaxDtos;

import java.math.BigDecimal;

import com.example.trip_sheet_backend.models.Tax.TaxType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTaxRequestDto {

  @NotBlank(message = "taxName is required")
  private String taxName;

  @NotNull(message = "taxPercentage is required")
  @DecimalMin(value = "0.00", inclusive = false, message = "taxPercentage must be greater than 0")
  private BigDecimal taxPercentage;

  @NotNull(message = "taxType is required")
  private TaxType taxType;
}
