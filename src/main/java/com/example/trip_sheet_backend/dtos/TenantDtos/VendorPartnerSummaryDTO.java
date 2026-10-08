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
  private String type;
  private VendorPartner.ContractStatus contractStatus;
  private Boolean isActive;
  private Boolean isAssociateSupplier;
  private Boolean isAssociateCustomer;
  private Tenant partnerVendor;

  public static VendorPartnerSummaryDTO fromEntity(VendorPartner vendorPartner, Tenant currentVendor) {
    Tenant connectedVendor = vendorPartner.getPrimaryVendor().getId().equals(currentVendor.getId())
        ? vendorPartner.getPartnerVendor()
        : vendorPartner.getPrimaryVendor();
    String type = Boolean.TRUE.equals(vendorPartner.getIsAssociateCustomer())
        ? "ASSOCIATE_CUSTOMER"
        : Boolean.TRUE.equals(vendorPartner.getIsAssociateSupplier())
            ? "ASSOCIATE_SUPPLIER"
            : "VENDOR";

    return new VendorPartnerSummaryDTO(
        vendorPartner.getId(),
        type,
        vendorPartner.getContractStatus(),
        vendorPartner.getIsActive(),
        vendorPartner.getIsAssociateSupplier(),
        vendorPartner.getIsAssociateCustomer(),
        connectedVendor
    );
  }
}
