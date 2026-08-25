package com.example.trip_sheet_backend.dtos.VendorOrganisationDtos;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorOrganisationTaxUpdateRequestDTO {
  @NotNull(message = "taxIds is required")
  private List<@NotNull(message = "Tax id cannot be null") UUID> taxIds;
}
