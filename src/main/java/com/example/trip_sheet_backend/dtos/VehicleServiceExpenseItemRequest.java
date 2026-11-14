package com.example.trip_sheet_backend.dtos;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleServiceExpenseItemRequest {
  @Size(max = 180)
  private String itemName;

  @Min(1)
  private Integer quantity;

  @NotNull
  @DecimalMin("0.00")
  @Digits(integer = 10, fraction = 2)
  private BigDecimal value;

  private Boolean discount = false;
}
