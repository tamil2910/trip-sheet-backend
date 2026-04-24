package com.example.trip_sheet_backend.services.CustomFieldService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.CustomField;
import com.example.trip_sheet_backend.models.Tenant;

public interface CustomFieldService {

  CustomField createForOrganisation(String name, Tenant organisationTenant, UUID createdBy);

  List<CustomField> getByOrganisationForVendor(Tenant vendorTenant, UUID organisationTenantId);

  List<CustomField> getByOrganisation(Tenant organisationTenant);

  CustomField updateForOrganisation(UUID customFieldId, String name, Tenant organisationTenant, UUID updatedBy);
}
