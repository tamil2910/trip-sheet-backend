package com.example.trip_sheet_backend.dtos.TripChargesDtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TripChargesCreateRequestDto {
  @NotNull(message = "Trip id is required")
  private String tripId;

  private String type;
  private String receiptImageUrl;

  @NotNull(message = "Amount is required")
  @Positive(message = "Amount must be a valid number")
  private Long amount;
  
  private String description;
}
