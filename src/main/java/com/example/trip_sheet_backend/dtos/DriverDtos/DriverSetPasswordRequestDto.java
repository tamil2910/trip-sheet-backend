package com.example.trip_sheet_backend.dtos.DriverDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverSetPasswordRequestDto {

  @NotBlank(message = "Password is required")
  private String password;
}