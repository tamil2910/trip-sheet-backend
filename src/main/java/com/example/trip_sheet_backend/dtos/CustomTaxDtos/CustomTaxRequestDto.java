package com.example.trip_sheet_backend.dtos.CustomTaxDtos;

import java.math.BigDecimal;

import com.example.trip_sheet_backend.models.Tax.TaxType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomTaxRequestDto {
  @NotBlank(message = "customTaxName is required")
  private String customTaxName;

  @NotNull(message = "taxPercentage is required")
  @DecimalMin(value = "0.00", inclusive = false, message = "taxPercentage must be greater than 0")
  private BigDecimal taxPercentage;

  @NotNull(message = "taxType is required")
  private TaxType taxType;

}
