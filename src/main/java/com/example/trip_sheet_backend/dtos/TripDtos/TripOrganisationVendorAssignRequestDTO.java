package com.example.trip_sheet_backend.dtos.TripDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripOrganisationVendorAssignRequestDTO {

  @NotBlank(message = "vendorId is required")
  private String vendorId;
}
