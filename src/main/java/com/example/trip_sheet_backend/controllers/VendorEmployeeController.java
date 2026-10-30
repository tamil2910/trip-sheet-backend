package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.VendorEmployeeDtos.VendorEmployeeCreateRequestDto;
import com.example.trip_sheet_backend.dtos.VendorEmployeeDtos.VendorEmployeeResponseDto;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.VendorEmployeeService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/vendor-employees")
public class VendorEmployeeController {
    private final VendorEmployeeService service;

    public VendorEmployeeController(VendorEmployeeService service) {
        this.service = service;
    }

    @PreAuthorize("hasAuthority('CAN_CREATE_USERACCOUNT')")
    @PostMapping
    public ResponseEntity<ApiResponse<VendorEmployeeResponseDto>> create(
            @Valid @RequestBody VendorEmployeeCreateRequestDto body,
            HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        UserAccount currentUser = (UserAccount) request.getAttribute("user");
        UUID createdBy = currentUser == null ? null : currentUser.getId();
        VendorEmployeeResponseDto result = service.create(body, tenant, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Vendor employee created successfully", result));
    }
}
