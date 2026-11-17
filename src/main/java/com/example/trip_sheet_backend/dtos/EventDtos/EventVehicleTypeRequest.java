package com.example.trip_sheet_backend.dtos.EventDtos;

import java.util.UUID;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EventVehicleTypeRequest {
  @NotNull
  private UUID id;

  @Min(0)
  @JsonAlias("allowed_passenger")
  private int allowedPassenger;
}
