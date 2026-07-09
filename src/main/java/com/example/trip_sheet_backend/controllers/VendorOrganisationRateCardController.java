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

import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationContractApprovalRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardApprovalRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardBulkCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardBulkReviewRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardResponseDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.models.VendorOrganisationRateCard;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.VendorOrganisationRateCardService.VendorOrganisationRateCardServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/vendor-organisation-rate-cards")
public class VendorOrganisationRateCardController {

  private final VendorOrganisationRateCardServiceImp vendorOrganisationRateCardService;

  public VendorOrganisationRateCardController(VendorOrganisationRateCardServiceImp vendorOrganisationRateCardService) {
    this.vendorOrganisationRateCardService = vendorOrganisationRateCardService;
  }

  @PostMapping("/create")
    public ResponseEntity<ApiResponse<VendorOrganisationRateCardResponseDTO>> createRateCard(
      HttpServletRequest request,
      @Valid @RequestBody VendorOrganisationRateCardBulkCreateRequestDTO body
  ) {
    UUID createdBy = (UUID) request.getAttribute("createdBy");
    Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");

        List<VendorOrganisationRateCard> createdRateCards = vendorOrganisationRateCardService
                .createRateCards(body, loggedInTenant, createdBy);

        VendorOrganisationRateCardResponseDTO response = createdRateCards.stream()
                .findFirst()
                .map(VendorOrganisationRateCard::getVendorOrganisation)
                .map(VendorOrganisationRateCardResponseDTO::fromVendorOrganisation)
                .orElseThrow(() -> new RuntimeException("No rate cards were created"));

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ApiResponse<>(
            true,
            "Vendor organisation rate cards created successfully",
            response
        ));
  }

  @PutMapping("/{rateCardId}/approve")
  public ResponseEntity<ApiResponse<VendorOrganisationRateCardResponseDTO>> approveRateCard(
      @PathVariable UUID rateCardId,
      HttpServletRequest request,
      @Valid @RequestBody VendorOrganisationRateCardApprovalRequestDTO body
  ) {
        return reviewRateCardInternal(rateCardId, request, body);
    }

    @PutMapping("/{rateCardId}/review")
    public ResponseEntity<ApiResponse<VendorOrganisationRateCardResponseDTO>> reviewRateCard(
            @PathVariable UUID rateCardId,
            HttpServletRequest request,
            @Valid @RequestBody VendorOrganisationRateCardApprovalRequestDTO body
    ) {
        return reviewRateCardInternal(rateCardId, request, body);
    }

    private ResponseEntity<ApiResponse<VendorOrganisationRateCardResponseDTO>> reviewRateCardInternal(
            UUID rateCardId,
            HttpServletRequest request,
            VendorOrganisationRateCardApprovalRequestDTO body
    ) {
        UUID approvedBy = (UUID) request.getAttribute("createdBy");
        Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");

        VendorOrganisationRateCard rateCard = vendorOrganisationRateCardService.reviewRateCard(rateCardId, body, loggedInTenant, approvedBy);

    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Vendor organisation rate card reviewed successfully",
        VendorOrganisationRateCardResponseDTO.fromEntity(rateCard)
    ));
  }

    @PutMapping("/{rateCardId}/update")
    public ResponseEntity<ApiResponse<VendorOrganisationRateCardResponseDTO>> updateRateCard(
            @PathVariable UUID rateCardId,
            HttpServletRequest request,
            @Valid @RequestBody VendorOrganisationRateCardUpdateRequestDTO body
    ) {
        UUID updatedBy = (UUID) request.getAttribute("createdBy");
        Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");

        VendorOrganisationRateCard rateCard = vendorOrganisationRateCardService.updateRateCard(
                rateCardId,
                body,
                loggedInTenant,
                updatedBy
        );

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Vendor organisation rate card updated successfully",
                VendorOrganisationRateCardResponseDTO.fromEntity(rateCard)
        ));
    }

  @PutMapping("/vendor-organisation/{vendorOrganisationId}/bulk-review")
  public ResponseEntity<ApiResponse<VendorOrganisationRateCardResponseDTO>> bulkReviewRateCards(
      @PathVariable UUID vendorOrganisationId,
      HttpServletRequest request,
      @Valid @RequestBody VendorOrganisationRateCardBulkReviewRequestDTO body
  ) {
    UUID actedBy = (UUID) request.getAttribute("createdBy");
    Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");

    VendorOrganisation vendorOrganisation = vendorOrganisationRateCardService.bulkReviewRateCards(
        vendorOrganisationId,
        body,
        loggedInTenant,
        actedBy
    );

    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Vendor organisation rate cards processed successfully",
        VendorOrganisationRateCardResponseDTO.fromVendorOrganisation(vendorOrganisation)
    ));
  }

  @GetMapping("/vendor-organisation/{vendorOrganisationId}")
  public ResponseEntity<ApiResponse<?>> getRateCardsByVendorOrganisation(
      @PathVariable UUID vendorOrganisationId,
      @RequestParam(required = false) String contractStatus,
      HttpServletRequest request
  ) {
    Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");

    if (contractStatus != null && contractStatus.equalsIgnoreCase("active")) {
      VendorOrganisationRateCard activeRateCard = vendorOrganisationRateCardService
          .getActiveRateCardByVendorOrganisation(vendorOrganisationId, loggedInTenant);

      return ResponseEntity.ok(new ApiResponse<>(
          true,
          "Active vendor organisation rate card fetched successfully",
          activeRateCard == null ? null : VendorOrganisationRateCardResponseDTO.fromEntity(activeRateCard)
      ));
    }

    VendorOrganisationRateCardResponseDTO response = vendorOrganisationRateCardService
        .getRateCardsByVendorOrganisation(vendorOrganisationId, loggedInTenant)
        .stream()
        .findFirst()
        .map(VendorOrganisationRateCard::getVendorOrganisation)
        .map(VendorOrganisationRateCardResponseDTO::fromVendorOrganisation)
        .orElse(null);

    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Vendor organisation rate cards fetched successfully",
        response
    ));
  }

    @PutMapping("/{vendorOrgId}/approve-contract")
    public ResponseEntity<ApiResponse<VendorOrganisation>> approveContract(
        @PathVariable UUID vendorOrgId,
        HttpServletRequest request,
        @Valid @RequestBody VendorOrganisationContractApprovalRequestDTO body
    ) {
        UUID approvedBy = (UUID) request.getAttribute("createdBy");
        Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");

        VendorOrganisation result = vendorOrganisationRateCardService.updateContractStatus(
            vendorOrgId,
            body,
            loggedInTenant,
            approvedBy
        );

        return ResponseEntity.ok(new ApiResponse<>(
            true,
            "Vendor organisation contract approved successfully",
            result
        ));
    }
}
