package com.example.trip_sheet_backend.dtos;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class VehicleServiceProgressUpdateDtos {
  private VehicleServiceProgressUpdateDtos() {}

  public record Request(
      @NotBlank @Size(max = 4000) String description,
      String photoBase64,
      String photoContentType) {}

  public record Response(
      UUID id,
      UUID vehicleServiceId,
      String description,
      String photoUrl,
      String updatedBy,
      Long updatedAt) {}
}
