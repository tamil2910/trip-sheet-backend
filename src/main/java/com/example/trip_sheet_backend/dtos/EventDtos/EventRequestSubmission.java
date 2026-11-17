package com.example.trip_sheet_backend.dtos.EventDtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EventRequestSubmission {
  @NotBlank
  @Size(max = 255)
  private String name;

  @Email
  @Size(max = 320)
  private String email;

  @Size(max = 32)
  private String phone;

  @Size(max = 1000)
  private String pickupAddress;

  @NotNull
  @DecimalMin("-90.0")
  @DecimalMax("90.0")
  private Double pickupLatitude;

  @NotNull
  @DecimalMin("-180.0")
  @DecimalMax("180.0")
  private Double pickupLongitude;

  @Size(max = 2000)
  private String notes;
}