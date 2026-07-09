package com.example.trip_sheet_backend.services.VendorOrganisationRateCardService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardApprovalRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardBulkCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardBulkReviewRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.models.VendorOrganisationRateCard;

public interface VendorOrganisationRateCardService {
  List<VendorOrganisationRateCard> createRateCards(
      VendorOrganisationRateCardBulkCreateRequestDTO body,
      Tenant loggedInTenant,
      UUID createdBy
  );

    VendorOrganisationRateCard updateRateCard(
      UUID rateCardId,
      VendorOrganisationRateCardUpdateRequestDTO body,
      Tenant loggedInTenant,
      UUID updatedBy
    );

  VendorOrganisationRateCard reviewRateCard(
      UUID rateCardId,
      VendorOrganisationRateCardApprovalRequestDTO body,
      Tenant loggedInTenant,
      UUID approvedBy
  );

  VendorOrganisation bulkReviewRateCards(
      UUID vendorOrganisationId,
      VendorOrganisationRateCardBulkReviewRequestDTO body,
      Tenant loggedInTenant,
      UUID actedBy
  );

  List<VendorOrganisationRateCard> getRateCardsByVendorOrganisation(UUID vendorOrganisationId, Tenant loggedInTenant);
  VendorOrganisationRateCard getActiveRateCardByVendorOrganisation(UUID vendorOrganisationId, Tenant loggedInTenant);
}
