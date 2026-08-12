package com.example.trip_sheet_backend.dtos.PurchaseOrderDtos;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PurchaseOrderAllocationRequestDTO {
  private UUID customFieldId;

  @NotBlank
  private String allocationKey;

  @NotNull
  @DecimalMin(value = "0.01")
  private BigDecimal sharePercent;
}
