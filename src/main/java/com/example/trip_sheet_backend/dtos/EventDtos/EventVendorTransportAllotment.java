package com.example.trip_sheet_backend.dtos.EventDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EventVendorTransportAllotment {
  @NotBlank private String driverId;
  @NotBlank private String vehicleId;
}
