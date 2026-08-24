package com.example.trip_sheet_backend.services.VendorOrganisationService;

import java.util.UUID;

import com.example.trip_sheet_backend.dtos.VendorOrganisationDtos.VendorOrganisationUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorOrganisation;

public interface VendorOrganisationService {
  VendorOrganisation update(UUID vendorOrganisationId, VendorOrganisationUpdateRequestDTO body,
      Tenant loggedInTenant, UUID updatedBy);
}
