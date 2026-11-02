package com.example.trip_sheet_backend.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.TenantJoinRequestDtos.CreateTenantJoinRequestDto;
import com.example.trip_sheet_backend.dtos.TenantJoinRequestDtos.DecideTenantJoinRequestDto;
import com.example.trip_sheet_backend.dtos.TenantJoinRequestDtos.TenantJoinRequestResponseDto;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.TenantJoinRequestService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/tenant-join-requests")
public class TenantJoinRequestController {
    private final TenantJoinRequestService service;

    public TenantJoinRequestController(TenantJoinRequestService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<ApiResponse<TenantJoinRequestResponseDto>> create(
            @Valid @RequestBody CreateTenantJoinRequestDto body,
            HttpServletRequest request) {
        UserAccount user = (UserAccount) request.getAttribute("user");
        TenantJoinRequestResponseDto result = service.create(user, body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Tenant membership request submitted", result));
    }

    @DeleteMapping("/my-tenant")
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<ApiResponse<Void>> unlinkMyTenant(HttpServletRequest request) {
        UserAccount user = (UserAccount) request.getAttribute("user");
        service.unlinkCurrentTenant(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Unlinked from tenant", null));
    }
    @GetMapping("/my")
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyRequests(
            Pageable pageable,
            HttpServletRequest request) {
        UserAccount user = (UserAccount) request.getAttribute("user");
        Page<TenantJoinRequestResponseDto> page = service.getMyRequests(user, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Your tenant requests", pageData(page)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('GUEST_TENANT_LINK_APPROVER') or hasAuthority('CAN_READ_TENANTJOINREQUEST')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPending(
            Pageable pageable,
            HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        Page<TenantJoinRequestResponseDto> page = service.getPending(tenant, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Pending tenant requests", pageData(page)));
    }

    @PatchMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('GUEST_TENANT_LINK_APPROVER')")
    public ResponseEntity<ApiResponse<TenantJoinRequestResponseDto>> approve(
            @PathVariable UUID requestId,
            @RequestBody DecideTenantJoinRequestDto body,
            HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        UserAccount admin = (UserAccount) request.getAttribute("user");
        TenantJoinRequestResponseDto result = service.approve(tenant, requestId, admin, body);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tenant request approved", result));
    }

    @PatchMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('GUEST_TENANT_LINK_APPROVER')")
    public ResponseEntity<ApiResponse<TenantJoinRequestResponseDto>> reject(
            @PathVariable UUID requestId,
            @RequestBody(required = false) DecideTenantJoinRequestDto body,
            HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        UserAccount admin = (UserAccount) request.getAttribute("user");
        TenantJoinRequestResponseDto result = service.reject(tenant, requestId, admin, body);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tenant request rejected", result));
    }

    private Map<String, Object> pageData(Page<TenantJoinRequestResponseDto> page) {
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
        return data;
    }
}
