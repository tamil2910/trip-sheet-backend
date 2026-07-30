package com.example.trip_sheet_backend.dtos.VendorOrganisationTaxDtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.trip_sheet_backend.models.Tax;
import com.example.trip_sheet_backend.models.VendorOrganisationTax;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VendorOrganisationTaxResponseDto {
  private UUID id;
  private UUID tenantId;
  private UUID vendorOrganisationId;
  private UUID taxId;
  private BigDecimal taxPercentage;
  private Tax.TaxType taxType;
  private String taxName;

  public static VendorOrganisationTaxResponseDto fromEntity(VendorOrganisationTax entity) {
    Tax tax = entity.getTax();
    return new VendorOrganisationTaxResponseDto(
        entity.getId(),
        entity.getTenant() == null ? null : entity.getTenant().getId(),
        entity.getVendorOrganisation() == null ? null : entity.getVendorOrganisation().getId(),
        tax == null ? null : tax.getId(),
        tax == null ? null : tax.getTaxPercentage(),
        tax == null ? null : tax.getTaxType(),
        tax == null ? null : tax.getTaxName()
    );
  }
}
