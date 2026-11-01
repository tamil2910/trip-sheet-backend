package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.VendorEmployeeAddressDtos.VendorEmployeeAddressRequestDto;
import com.example.trip_sheet_backend.dtos.VendorEmployeeAddressDtos.VendorEmployeeAddressResponseDto;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.VendorEmployeeAddressService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/vendor-employees/{employeeId}/addresses")
public class VendorEmployeeAddressController {
    private final VendorEmployeeAddressService addressService;

    public VendorEmployeeAddressController(VendorEmployeeAddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CAN_CREATE_USERACCOUNT')")
    public ResponseEntity<ApiResponse<VendorEmployeeAddressResponseDto>> create(
            @PathVariable UUID employeeId,
            @Valid @RequestBody VendorEmployeeAddressRequestDto body,
            HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        UserAccount user = (UserAccount) request.getAttribute("user");
        VendorEmployeeAddressResponseDto result = addressService.create(
                tenant, employeeId, user == null ? null : user.getId(), body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Vendor employee address created", result));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAN_READ_USERACCOUNT')")
    public ResponseEntity<ApiResponse<List<VendorEmployeeAddressResponseDto>>> list(
            @PathVariable UUID employeeId,
            HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        return ResponseEntity.ok(new ApiResponse<>(true, "Vendor employee addresses found",
                addressService.list(tenant, employeeId)));
    }

    @PutMapping("/{addressId}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_USERACCOUNT')")
    public ResponseEntity<ApiResponse<VendorEmployeeAddressResponseDto>> update(
            @PathVariable UUID employeeId,
            @PathVariable UUID addressId,
            @Valid @RequestBody VendorEmployeeAddressRequestDto body,
            HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        UserAccount user = (UserAccount) request.getAttribute("user");
        VendorEmployeeAddressResponseDto result = addressService.update(
                tenant, employeeId, addressId, user == null ? null : user.getId(), body);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vendor employee address updated", result));
    }

    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasAuthority('CAN_DELETE_USERACCOUNT')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID employeeId,
            @PathVariable UUID addressId,
            HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        addressService.delete(tenant, employeeId, addressId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vendor employee address deleted", null));
    }
}
