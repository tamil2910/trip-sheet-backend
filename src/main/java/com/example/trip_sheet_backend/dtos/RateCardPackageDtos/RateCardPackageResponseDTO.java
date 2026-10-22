package com.example.trip_sheet_backend.dtos.RateCardPackageDtos;

import java.util.List;
import java.util.UUID;
import com.example.trip_sheet_backend.models.ratecardpackage.RateCardPackage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RateCardPackageResponseDTO {
  private UUID id;
  private String name;
  private UUID vendorPartnerId;
  private UUID vendorOrganisationId;
  private List<UUID> rateCardIds;
  private Long validFrom;
  private Long validTo;
  private Boolean isDefault;

  public static RateCardPackageResponseDTO fromEntity(RateCardPackage entity) {
    List<UUID> ids = entity.getItems().stream().map(item -> item.getVendorPartnerRateCard() != null
        ? item.getVendorPartnerRateCard().getId() : item.getVendorOrganisationRateCard().getId()).toList();
    return new RateCardPackageResponseDTO(entity.getId(), entity.getName(),
        entity.getVendorPartner() == null ? null : entity.getVendorPartner().getId(),
        entity.getVendorOrganisation() == null ? null : entity.getVendorOrganisation().getId(),
        ids, entity.getValidFrom(), entity.getValidTo(), entity.getIsDefault());
  }
}
