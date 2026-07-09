package com.example.trip_sheet_backend.services.VendorPartnerRateCardService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardApprovalRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardBulkCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardBulkReviewRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.models.VendorPartnerRateCard;

public interface VendorPartnerRateCardService {
  List<VendorPartnerRateCard> createRateCards(VendorPartnerRateCardBulkCreateRequestDTO body, Tenant loggedInTenant, UUID createdBy);
  VendorPartnerRateCard updateRateCard(UUID rateCardId, VendorPartnerRateCardUpdateRequestDTO body, Tenant loggedInTenant, UUID updatedBy);
  VendorPartnerRateCard reviewRateCard(UUID rateCardId, VendorPartnerRateCardApprovalRequestDTO body, Tenant loggedInTenant, UUID approvedBy);
  VendorPartner deleteRateCard(UUID rateCardId, Tenant loggedInTenant, UUID deletedBy);
  VendorPartner bulkReviewRateCards(UUID vendorPartnerId, VendorPartnerRateCardBulkReviewRequestDTO body, Tenant loggedInTenant, UUID actedBy);
  List<VendorPartnerRateCard> getRateCardsByVendorPartner(UUID vendorPartnerId, Tenant loggedInTenant);
  VendorPartnerRateCard getActiveRateCardByVendorPartner(UUID vendorPartnerId, Tenant loggedInTenant);
}
