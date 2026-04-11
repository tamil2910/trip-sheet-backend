package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardApprovalRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardBulkCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardResponseDTO;
import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorPartnerRateCard;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.VendorPartnerRateCardService.VendorPartnerRateCardServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/vendor-partner-rate-cards")
public class VendorPartnerRateCardController {

  private final VendorPartnerRateCardServiceImp vendorPartnerRateCardService;

  public VendorPartnerRateCardController(VendorPartnerRateCardServiceImp vendorPartnerRateCardService) {
    this.vendorPartnerRateCardService = vendorPartnerRateCardService;
  }

  @PostMapping("/create")
  public ResponseEntity<ApiResponse<List<VendorPartnerRateCardResponseDTO>>> createRateCard(
      HttpServletRequest request,
      @Valid @RequestBody VendorPartnerRateCardBulkCreateRequestDTO body
  ) {
    UUID createdBy = (UUID) request.getAttribute("createdBy");
    Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");

    List<VendorPartnerRateCardResponseDTO> response = vendorPartnerRateCardService
        .createRateCards(body, loggedInTenant, createdBy)
        .stream()
        .map(VendorPartnerRateCardResponseDTO::fromEntity)
        .toList();

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ApiResponse<>(
            true,
            "Vendor partner rate cards created successfully",
            response
        ));
  }

  @PutMapping("/{rateCardId}/approve")
  public ResponseEntity<ApiResponse<VendorPartnerRateCardResponseDTO>> approveRateCard(
      @PathVariable UUID rateCardId,
      HttpServletRequest request,
      @Valid @RequestBody VendorPartnerRateCardApprovalRequestDTO body
  ) {
    UUID approvedBy = (UUID) request.getAttribute("createdBy");
    Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");

    VendorPartnerRateCard rateCard = vendorPartnerRateCardService.reviewRateCard(
        rateCardId,
        body,
        loggedInTenant,
        approvedBy
    );

    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Vendor partner rate card reviewed successfully",
        VendorPartnerRateCardResponseDTO.fromEntity(rateCard)
    ));
  }

    @PutMapping("/{rateCardId}/update")
    public ResponseEntity<ApiResponse<VendorPartnerRateCardResponseDTO>> updateRateCard(
            @PathVariable UUID rateCardId,
            HttpServletRequest request,
            @Valid @RequestBody VendorPartnerRateCardUpdateRequestDTO body
    ) {
        UUID updatedBy = (UUID) request.getAttribute("createdBy");
        Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");

        VendorPartnerRateCard rateCard = vendorPartnerRateCardService.updateRateCard(
                rateCardId,
                body,
                loggedInTenant,
                updatedBy
        );

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Vendor partner rate card updated successfully",
                VendorPartnerRateCardResponseDTO.fromEntity(rateCard)
        ));
    }

  @GetMapping("/vendor-partner/{vendorPartnerId}")
  public ResponseEntity<ApiResponse<?>> getRateCardsByVendorPartner(
      @PathVariable UUID vendorPartnerId,
      @RequestParam(required = false) String contractStatus,
      HttpServletRequest request
  ) {
    Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");

    if (contractStatus != null && contractStatus.equalsIgnoreCase("active")) {
      VendorPartnerRateCard activeRateCard = vendorPartnerRateCardService
          .getActiveRateCardByVendorPartner(vendorPartnerId, loggedInTenant);

      return ResponseEntity.ok(new ApiResponse<>(
          true,
          "Active vendor partner rate card fetched successfully",
          activeRateCard == null ? null : VendorPartnerRateCardResponseDTO.fromEntity(activeRateCard)
      ));
    }

    List<VendorPartnerRateCardResponseDTO> response = vendorPartnerRateCardService
        .getRateCardsByVendorPartner(vendorPartnerId, loggedInTenant)
        .stream()
        .map(VendorPartnerRateCardResponseDTO::fromEntity)
        .toList();

    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Vendor partner rate cards fetched successfully",
        response
    ));
  }
}
