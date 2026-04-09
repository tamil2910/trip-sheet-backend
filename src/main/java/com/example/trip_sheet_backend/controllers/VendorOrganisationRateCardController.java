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
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardApprovalRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardBulkCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardResponseDTO;
import com.example.trip_sheet_backend.models.Tenant;
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
  public ResponseEntity<ApiResponse<List<VendorOrganisationRateCardResponseDTO>>> createRateCard(
      HttpServletRequest request,
      @Valid @RequestBody VendorOrganisationRateCardBulkCreateRequestDTO body
  ) {
    UUID createdBy = (UUID) request.getAttribute("createdBy");
    Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");

    List<VendorOrganisationRateCardResponseDTO> response = vendorOrganisationRateCardService
        .createRateCards(body, loggedInTenant, createdBy)
        .stream()
        .map(VendorOrganisationRateCardResponseDTO::fromEntity)
        .toList();

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
    UUID approvedBy = (UUID) request.getAttribute("createdBy");
    Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");

    VendorOrganisationRateCard rateCard = vendorOrganisationRateCardService.reviewRateCard(
        rateCardId,
        body,
        loggedInTenant,
        approvedBy
    );

    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Vendor organisation rate card reviewed successfully",
        VendorOrganisationRateCardResponseDTO.fromEntity(rateCard)
    ));
  }

  @GetMapping("/vendor-organisation/{vendorOrganisationId}")
  public ResponseEntity<ApiResponse<List<VendorOrganisationRateCardResponseDTO>>> getRateCardsByVendorOrganisation(
      @PathVariable UUID vendorOrganisationId,
      HttpServletRequest request
  ) {
    Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");

    List<VendorOrganisationRateCardResponseDTO> response = vendorOrganisationRateCardService
        .getRateCardsByVendorOrganisation(vendorOrganisationId, loggedInTenant)
        .stream()
        .map(VendorOrganisationRateCardResponseDTO::fromEntity)
        .toList();

    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Vendor organisation rate cards fetched successfully",
        response
    ));
  }
}
