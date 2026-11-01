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
import com.example.trip_sheet_backend.models.VendorEmployee;
import com.example.trip_sheet_backend.repositories.VendorEmployeeRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.VendorEmployeeAddressService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/vendor-employees/me/addresses")
@PreAuthorize("isAuthenticated()")
public class VendorEmployeeSelfAddressController {
    private final VendorEmployeeRepository employeeRepository;
    private final VendorEmployeeAddressService addressService;

    public VendorEmployeeSelfAddressController(
            VendorEmployeeRepository employeeRepository,
            VendorEmployeeAddressService addressService) {
        this.employeeRepository = employeeRepository;
        this.addressService = addressService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VendorEmployeeAddressResponseDto>> create(
            @Valid @RequestBody VendorEmployeeAddressRequestDto body,
            HttpServletRequest request) {
        EmployeeContext context = getCurrentEmployee(request);
        VendorEmployeeAddressResponseDto result = addressService.create(
                context.tenant(), context.employee().getId(), context.user().getId(), body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Vendor employee address created", result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VendorEmployeeAddressResponseDto>>> list(HttpServletRequest request) {
        EmployeeContext context = getCurrentEmployee(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vendor employee addresses found",
                addressService.list(context.tenant(), context.employee().getId())));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<VendorEmployeeAddressResponseDto>> update(
            @PathVariable UUID addressId,
            @Valid @RequestBody VendorEmployeeAddressRequestDto body,
            HttpServletRequest request) {
        EmployeeContext context = getCurrentEmployee(request);
        VendorEmployeeAddressResponseDto result = addressService.update(
                context.tenant(), context.employee().getId(), addressId, context.user().getId(), body);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vendor employee address updated", result));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID addressId,
            HttpServletRequest request) {
        EmployeeContext context = getCurrentEmployee(request);
        addressService.delete(context.tenant(), context.employee().getId(), addressId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vendor employee address deleted", null));
    }

    private EmployeeContext getCurrentEmployee(HttpServletRequest request) {
        UserAccount user = (UserAccount) request.getAttribute("user");
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        if (user == null || tenant == null || tenant.getTenantType() != Tenant.TenantType.VENDOR) {
            throw new RuntimeException("Authenticated vendor employee context is required");
        }
        VendorEmployee employee = employeeRepository.findByAccount_IdAndIsUserTrue(user.getId())
                .filter(candidate -> candidate.getTenant() != null
                        && tenant.getId().equals(candidate.getTenant().getId()))
                .orElseThrow(() -> new RuntimeException("Vendor employee profile not found for current user"));
        return new EmployeeContext(user, tenant, employee);
    }

    private record EmployeeContext(UserAccount user, Tenant tenant, VendorEmployee employee) {
    }
}
