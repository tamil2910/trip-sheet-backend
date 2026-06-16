package com.example.trip_sheet_backend.dtos.TripBillingAllocationDtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.trip_sheet_backend.models.TripBillingAllocation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripBillingAllocationUpdateRequestDTO {

  @NotNull(message = "tripId is required")
  private UUID tripId;

  @NotNull(message = "allocationType is required")
  private TripBillingAllocation.AllocationType allocationType;

  @NotBlank(message = "allocationKey is required")
  private String allocationKey;

  @NotNull(message = "sharePercent is required")
  @DecimalMin(value = "0.00", inclusive = true, message = "sharePercent cannot be negative")
  private BigDecimal sharePercent;

  @NotNull(message = "shareAmount is required")
  @DecimalMin(value = "0.00", inclusive = true, message = "shareAmount cannot be negative")
  private BigDecimal shareAmount;

  @NotNull(message = "status is required")
  private TripBillingAllocation.AllocationStatus status;
}
