package com.example.trip_sheet_backend.dtos.VendorOrganisationDtos;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.VendorOrganisation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VendorOrganisationResponseDTO {
  private UUID id;
  private UUID vendorId;
  private UUID organisationId;
  private Boolean active;
  private Long onboardedAt;
  private Integer paymentTimelineInDays;
  private String localBillingStructure;
  private Integer minGtgKmLimit;
  private Integer minGtgHrLimit;
  private Integer maxGtgKmLimit;
  private Integer maxGtgHrLimit;
  private VendorOrganisation.ContractStatus contractStatus;
  private Long contractStartDate;
  private Long contractEndDate;
  private List<UUID> taxIds;

  public static VendorOrganisationResponseDTO fromEntity(VendorOrganisation entity) {
    return new VendorOrganisationResponseDTO(
        entity.getId(), entity.getVendor().getId(), entity.getOrganisation().getId(),
        entity.getActive(), entity.getOnboardedAt(), entity.getPaymentTimelineInDays(),
        entity.getLocalBillingStructure(), entity.getMinGtgKmLimit(), entity.getMinGtgHrLimit(),
        entity.getMaxGtgKmLimit(), entity.getMaxGtgHrLimit(), entity.getContractStatus(),
        entity.getContractStartDate(), entity.getContractEndDate(),
        entity.getTaxList() == null ? List.of() : entity.getTaxList().stream().map(tax -> tax.getId()).toList());
  }
}
