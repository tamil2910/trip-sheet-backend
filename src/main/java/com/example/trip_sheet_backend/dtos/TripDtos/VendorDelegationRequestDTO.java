package com.example.trip_sheet_backend.dtos.TripDtos;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VendorDelegationRequestDTO {
  
  private String tripId;

  private String fromVendorId; // the delegator
  private String toVendorId;   // the receiver
}
