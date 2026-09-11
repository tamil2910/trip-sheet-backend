package com.example.trip_sheet_backend.dtos.VendorPartnerDtos;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorPartnerUpdateRequestDTO {
  private List<@NotNull(message = "Tax id cannot be null") UUID> taxIds;
}
