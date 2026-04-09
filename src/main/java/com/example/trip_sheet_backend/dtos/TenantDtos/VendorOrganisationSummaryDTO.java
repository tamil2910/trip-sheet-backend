package com.example.trip_sheet_backend.dtos.TenantDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorOrganisation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorOrganisationSummaryDTO {
  private UUID vendorOrganisationId;
  private VendorOrganisation.ContractStatus contractStatus;
  private Tenant organisation;
  private Tenant vendor;

  public static VendorOrganisationSummaryDTO fromEntity(VendorOrganisation vendorOrganisation) {
    return new VendorOrganisationSummaryDTO(
        vendorOrganisation.getId(),
        vendorOrganisation.getContractStatus(),
        vendorOrganisation.getOrganisation(),
        vendorOrganisation.getVendor()
    );
  }
}
