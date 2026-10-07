package com.example.trip_sheet_backend.dtos.TenantDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.Tenant;
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
public class MyClientDetailsDTO {
  private UUID vendorOrganisationId;
  private UUID vendorPartnerId;
  private String contractStatus;
  private String type;
  private Tenant organisation;
  private Tenant associateCustomer;

  public static MyClientDetailsDTO fromOrganisation(VendorOrganisation link) {
    return new MyClientDetailsDTO(
        link.getId(), null,
        link.getContractStatus() == null ? null : link.getContractStatus().name(),
        "ORGANISATION", link.getOrganisation(), null);
  }

  public static MyClientDetailsDTO fromAssociateCustomer(VendorPartner link) {
    return new MyClientDetailsDTO(
        null, link.getId(),
        link.getContractStatus() == null ? null : link.getContractStatus().name(),
        "ASSOCIATE_CUSTOMER", null, link.getPartnerVendor());
  }
}
