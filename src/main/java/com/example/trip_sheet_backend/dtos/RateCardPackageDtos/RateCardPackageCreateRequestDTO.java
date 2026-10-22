package com.example.trip_sheet_backend.dtos.RateCardPackageDtos;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RateCardPackageCreateRequestDTO {
  @NotBlank
  private String name;
  private UUID vendorPartnerId;
  private UUID vendorOrganisationId;
  @NotEmpty
  @Size(max = 500)
  private List<UUID> rateCardIds;
  private Long validFrom;
  private Long validTo;
  private Boolean isDefault = false;
}
