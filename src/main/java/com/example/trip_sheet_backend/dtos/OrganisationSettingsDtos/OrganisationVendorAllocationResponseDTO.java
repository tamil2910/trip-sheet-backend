package com.example.trip_sheet_backend.dtos.OrganisationSettingsDtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.trip_sheet_backend.models.OrganisationVendorAllocation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrganisationVendorAllocationResponseDTO {
  private UUID vendorOrganisationId;
  private UUID vendorId;
  private String vendorName;
  private BigDecimal allocationPercentage;

  public static OrganisationVendorAllocationResponseDTO fromEntity(OrganisationVendorAllocation allocation) {
    var relationship = allocation.getVendorOrganisation();
    var vendor = relationship == null ? null : relationship.getVendor();
    return new OrganisationVendorAllocationResponseDTO(
        relationship == null ? null : relationship.getId(),
        vendor == null ? null : vendor.getId(),
        vendor == null ? null : vendor.getTenantName(),
        allocation.getAllocationPercentage());
  }
}
