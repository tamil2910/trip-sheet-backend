package com.example.trip_sheet_backend.dtos.TripDtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripLocationRequestDTO {
  @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
  private Double latitude;

  @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
  private Double longitude;

  /** Unix epoch milliseconds from the device; server receive time is stored separately. */
  @NotNull @PositiveOrZero
  private Long recordedAt;

  @PositiveOrZero
  private Double accuracyMeters;

  @PositiveOrZero
  private Double speedMetersPerSecond;
}
