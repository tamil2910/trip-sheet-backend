package com.example.trip_sheet_backend.dtos.TripDtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TripLabelResponseDTO {
    private String id;
    private String name;
    private String color;
}
