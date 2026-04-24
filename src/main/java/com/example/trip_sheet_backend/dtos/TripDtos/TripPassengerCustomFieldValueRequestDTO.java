package com.example.trip_sheet_backend.dtos.TripDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripPassengerCustomFieldValueRequestDTO {

  @NotBlank(message = "passengerId is required")
  private String passengerId;

  @NotBlank(message = "customFieldId is required")
  private String customFieldId;

  private String value;
}
