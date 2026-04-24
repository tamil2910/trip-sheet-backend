package com.example.trip_sheet_backend.dtos.TripDtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripPassengerCustomFieldValueResponseDTO {

  private String id;
  private TripRelationResponseDTO passenger;
  private TripBasicRelationResponseDTO customField;
  private String value;
}
