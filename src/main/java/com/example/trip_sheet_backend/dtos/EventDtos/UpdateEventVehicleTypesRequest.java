package com.example.trip_sheet_backend.dtos.EventDtos;

import java.util.List;
import com.example.trip_sheet_backend.dtos.EventDtos.EventVehicleTypeRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateEventVehicleTypesRequest {
  @NotNull
  private List<@jakarta.validation.Valid EventVehicleTypeRequest> vehicleTypeIds;
}

