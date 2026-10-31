package com.example.trip_sheet_backend.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @PreAuthorize("hasAuthority('CAN_READ_USERACCOUNT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorEmployeeResponseDto>> getById(
            @PathVariable UUID id,
            HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        VendorEmployeeResponseDto result = service.getById(tenant, id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vendor employee found", result));
    }
    @PreAuthorize("hasAuthority('CAN_READ_USERACCOUNT')")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String employeeNumber,
            @RequestParam(required = false) Boolean isUser,
            Pageable pageable,
            HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        Page<VendorEmployeeResponseDto> page = service.search(
                tenant, query, name, email, phone, employeeNumber, isUser, pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("data", page.getContent());
        data.put("currentPage", page.getNumber());
        data.put("pageSize", page.getSize());
        data.put("currentPageCount", page.getNumberOfElements());
        data.put("totalItems", page.getTotalElements());
        data.put("totalPages", page.getTotalPages());
        data.put("isFirst", page.isFirst());
        data.put("isLast", page.isLast());
        data.put("hasNext", page.hasNext());
        data.put("hasPrevious", page.hasPrevious());
        return ResponseEntity.ok(new ApiResponse<>(true, "Vendor employees found", data));
    }
}
