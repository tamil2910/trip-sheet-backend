package com.example.trip_sheet_backend.dtos.TripBillingAllocationDtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.TripBillingAllocation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TripBillingAllocationResponseDTO {
  private UUID id;
  private UUID tripId;
  private String tripCode;
  private UUID tenantId;
  private TripBillingAllocation.AllocationType allocationType;
  private String allocationKey;
  private BigDecimal sharePercent;
  private BigDecimal shareAmount;
  private TripBillingAllocation.AllocationStatus status;

  public static TripBillingAllocationResponseDTO fromEntity(TripBillingAllocation entity) {
    Trip trip = entity.getTrip();
    return new TripBillingAllocationResponseDTO(
        entity.getId(),
        trip == null ? null : trip.getId(),
        trip == null ? null : trip.getTripCode(),
        entity.getTenant() == null ? null : entity.getTenant().getId(),
        entity.getAllocationType(),
        entity.getAllocationKey(),
        entity.getSharePercent(),
        entity.getShareAmount(),
        entity.getStatus()
    );
  }
}
