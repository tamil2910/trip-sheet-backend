package com.example.trip_sheet_backend.dtos.TripChargesDtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TripChargesBulkCreateRequestDto {

  @NotNull(message = "Trip id is required")
  private String tripId;

  @Valid
  @NotEmpty(message = "Atleast one charge item is required")
  private List<TripChargeItemDto> charges;
}
