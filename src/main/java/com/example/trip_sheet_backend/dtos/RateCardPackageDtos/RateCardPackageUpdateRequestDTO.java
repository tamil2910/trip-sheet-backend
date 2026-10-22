package com.example.trip_sheet_backend.dtos.RateCardPackageDtos;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RateCardPackageUpdateRequestDTO {
  private String name;
  @Size(max = 500)
  private List<UUID> rateCardIds;
  private Long validFrom;
  private Long validTo;
  private Boolean isDefault;
}
