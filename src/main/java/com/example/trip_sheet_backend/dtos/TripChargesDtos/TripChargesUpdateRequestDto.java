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
public class TripChargesUpdateRequestDto {
  // private String tripId;

  private String type;
  private String receiptImageUrl;

  @Positive(message = "Amount must be a valid number")
  private Long amount;
  
  private String description;
}
