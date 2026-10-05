package com.example.trip_sheet_backend.dtos.TripDtos;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripLabelAssignRequestDTO {
  @NotNull(message = "labelIds is required")
  private List<String> labelIds;
}
