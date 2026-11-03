package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.SupplierDriverVehiclesDtos.SupplierPartnerDriverResponseDTO;
import com.example.trip_sheet_backend.dtos.SupplierDriverVehiclesDtos.SupplierPartnerVehicleResponseDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.SupplierDriverVehiclesService.SupplierDriverVehiclesService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class SupplierPartnerResourcesController {
    private final SupplierDriverVehiclesService service;

    public SupplierPartnerResourcesController(SupplierDriverVehiclesService service) {
        this.service = service;
    }

    @GetMapping("/supplier-drivers/partner/{partnerVendorId}")
    public ResponseEntity<ApiResponse<List<SupplierPartnerDriverResponseDTO>>> getPartnerDrivers(
            @PathVariable UUID partnerVendorId, HttpServletRequest request) {
        Tenant primaryVendor = (Tenant) request.getAttribute("tenant");
        List<SupplierPartnerDriverResponseDTO> drivers = service.getDriversByPartner(primaryVendor, partnerVendorId)
                .stream().map(SupplierPartnerDriverResponseDTO::fromEntity).toList();
        return ResponseEntity.ok(new ApiResponse<>(true, "Partner drivers fetched successfully", drivers));
    }

    @GetMapping("/supplier-vehicles/partner/{partnerVendorId}")
    public ResponseEntity<ApiResponse<List<SupplierPartnerVehicleResponseDTO>>> getPartnerVehicles(
            @PathVariable UUID partnerVendorId, HttpServletRequest request) {
        Tenant primaryVendor = (Tenant) request.getAttribute("tenant");
        List<SupplierPartnerVehicleResponseDTO> vehicles = service.getVehiclesByPartner(primaryVendor, partnerVendorId)
                .stream().map(SupplierPartnerVehicleResponseDTO::fromEntity).toList();
        return ResponseEntity.ok(new ApiResponse<>(true, "Partner vehicles fetched successfully", vehicles));
    }
}
