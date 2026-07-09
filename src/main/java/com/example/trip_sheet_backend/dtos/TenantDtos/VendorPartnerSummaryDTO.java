package com.example.trip_sheet_backend.dtos.TenantDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorPartner;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorPartnerSummaryDTO {
  private UUID vendorPartnerId;
  private VendorPartner.ContractStatus contractStatus;
  private Tenant partnerVendor;

  public static VendorPartnerSummaryDTO fromEntity(VendorPartner vendorPartner) {
    return new VendorPartnerSummaryDTO(
        vendorPartner.getId(),
        vendorPartner.getContractStatus(),
        vendorPartner.getPartnerVendor()
    );
  }
}
