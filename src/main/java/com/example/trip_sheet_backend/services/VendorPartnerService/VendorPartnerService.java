package com.example.trip_sheet_backend.services.VendorPartnerService;

import java.util.UUID;

import com.example.trip_sheet_backend.dtos.VendorPartnerDtos.VendorPartnerUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorPartner;

public interface VendorPartnerService {
  VendorPartner update(UUID vendorPartnerId, VendorPartnerUpdateRequestDTO body,
      Tenant loggedInTenant, UUID updatedBy);
}
