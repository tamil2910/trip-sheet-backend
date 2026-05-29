package com.example.trip_sheet_backend.dtos.TripChargesDtos;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePhoneRequestDto {
  @Positive(message = "Phone number must be a valid number")
  private String phone;
}
