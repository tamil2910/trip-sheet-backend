package com.example.trip_sheet_backend.dtos.EventDtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EventVendorTransportDecision {
  @NotNull private Boolean accepted;
}
