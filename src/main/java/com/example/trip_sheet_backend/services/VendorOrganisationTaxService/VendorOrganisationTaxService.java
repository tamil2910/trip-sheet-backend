package com.example.trip_sheet_backend.services.VendorOrganisationTaxService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.dtos.VendorOrganisationTaxDtos.VendorOrganisationTaxCreateRequestDto;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorOrganisationTax;

public interface VendorOrganisationTaxService {
  VendorOrganisationTax createVendorOrganisationTax(
      VendorOrganisationTaxCreateRequestDto body,
      Tenant tokenTenant,
      UUID createdBy
  );

  List<VendorOrganisationTax> getVendorOrganisationTaxes(Tenant tokenTenant);

  List<VendorOrganisationTax> getVendorOrganisationTaxesByVendorOrganisation(UUID vendorOrganisationId, Tenant tokenTenant);

  VendorOrganisationTax getVendorOrganisationTaxById(UUID id, Tenant tokenTenant);

  VendorOrganisationTax updateVendorOrganisationTax(
      UUID id,
      VendorOrganisationTaxCreateRequestDto body,
      Tenant tokenTenant,
      UUID updatedBy
  );

  void deleteVendorOrganisationTax(UUID id, Tenant tokenTenant, UUID deletedBy);
}
