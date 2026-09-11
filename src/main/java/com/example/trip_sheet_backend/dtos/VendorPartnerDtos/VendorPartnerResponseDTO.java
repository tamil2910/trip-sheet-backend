package com.example.trip_sheet_backend.dtos.VendorPartnerDtos;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.VendorPartner;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VendorPartnerResponseDTO {
  private UUID id;
  private UUID primaryVendorId;
  private UUID partnerVendorId;
  private VendorPartner.ContractStatus contractStatus;
  private Long onboardedAt;
  private Integer paymentTimelineInDays;
  private String localBillingStructure;
  private Integer minGtgKmLimit;
  private Integer minGtgHrLimit;
  private Integer maxGtgKmLimit;
  private Integer maxGtgHrLimit;
  private Long contractStartDate;
  private Long contractEndDate;
  private List<UUID> taxIds;

  public static VendorPartnerResponseDTO fromEntity(VendorPartner entity) {
    return new VendorPartnerResponseDTO(
        entity.getId(), entity.getPrimaryVendor().getId(), entity.getPartnerVendor().getId(),
        entity.getContractStatus(), entity.getOnboardedAt(), entity.getPaymentTimelineInDays(),
        entity.getLocalBillingStructure(), entity.getMinGtgKmLimit(), entity.getMinGtgHrLimit(),
        entity.getMaxGtgKmLimit(), entity.getMaxGtgHrLimit(), entity.getContractStartDate(),
        entity.getContractEndDate(),
        entity.getTaxList() == null ? List.of() : entity.getTaxList().stream().map(tax -> tax.getId()).toList());
  }
}
