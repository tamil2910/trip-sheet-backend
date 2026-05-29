package com.example.trip_sheet_backend.dtos.TripChargesDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.TripCharges;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TripChargeResponseDto {
  private UUID id;
  private UUID tripId;
  private String type;
  private String receiptImageUrl;
  private Long amount;
  private String description;
  private UUID tenantId;

  public static TripChargeResponseDto fromEntity(TripCharges tripCharge) {
    if (tripCharge == null) {
      return null;
    }

    return new TripChargeResponseDto(
        tripCharge.getId(),
        tripCharge.getTrip() != null ? tripCharge.getTrip().getId() : null,
        tripCharge.getType() != null ? tripCharge.getType().name() : null,
        tripCharge.getReceiptImageUrl(),
        tripCharge.getAmount(),
        tripCharge.getDescription(),
        tripCharge.getTenant() != null ? tripCharge.getTenant().getId() : null);
  }
}