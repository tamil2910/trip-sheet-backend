package com.example.trip_sheet_backend.dtos.TenantDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MyClientSummaryDTO {
  private UUID vendorOrganisationId;
  private UUID vendorPartnerId;
  private VendorOrganisation.ContractStatus contractStatus;
  private String type;
  private UUID id;
  private String tenantName;

  public static MyClientSummaryDTO fromEntity(VendorOrganisation vendorOrganisation) {
    MyClientSummaryDTO dto = new MyClientSummaryDTO();
    dto.setVendorOrganisationId(vendorOrganisation.getId());
    dto.setContractStatus(vendorOrganisation.getContractStatus());
    dto.setType("ORGANISATION");
    if (vendorOrganisation.getOrganisation() != null) {
      dto.setId(vendorOrganisation.getOrganisation().getId());
      dto.setTenantName(vendorOrganisation.getOrganisation().getTenantName());
    }
    return dto;
  }

  public static MyClientSummaryDTO fromAssociateCustomer(VendorPartner vendorPartner) {
    MyClientSummaryDTO dto = new MyClientSummaryDTO();
    dto.setVendorPartnerId(vendorPartner.getId());
    dto.setType("ASSOCIATE_CUSTOMER");
    if (vendorPartner.getPartnerVendor() != null) {
      dto.setId(vendorPartner.getPartnerVendor().getId());
      dto.setTenantName(vendorPartner.getPartnerVendor().getTenantName());
    }
    return dto;
  }

}
