package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.ShiftRequestDTO;
import com.example.trip_sheet_backend.dtos.ShiftResponseDTO;
import com.example.trip_sheet_backend.models.Shift;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.ShiftRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/shifts")
@Transactional
public class ShiftController {
    private final ShiftRepository shiftRepository;
    private final TenantRepository tenantRepository;
    private final VendorOrganisationRepository vendorOrganisationRepository;

    public ShiftController(ShiftRepository shiftRepository, TenantRepository tenantRepository,
            VendorOrganisationRepository vendorOrganisationRepository) {
        this.shiftRepository = shiftRepository;
        this.tenantRepository = tenantRepository;
        this.vendorOrganisationRepository = vendorOrganisationRepository;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CAN_CREATE_SHIFT')")
    public ResponseEntity<ApiResponse<ShiftResponseDTO>> create(
            @Valid @RequestBody ShiftRequestDTO body, HttpServletRequest request) {
        Tenant tenant = requireTenant(request);
        Tenant organisation = resolveOrganisation(body, tenant);
        Shift shift = new Shift();
        shift.setName(body.getName().trim());
        shift.setStartTime(body.getStartTime());
        shift.setEndTime(body.getEndTime());
        shift.setShiftType(body.getShiftType());
        shift.setTenant(tenant);
        shift.setOrganisation(organisation);
        Object createdBy = request.getAttribute("createdBy");
        if (createdBy != null) {
            shift.setCreatedBy(createdBy.toString());
            shift.setUpdatedBy(createdBy.toString());
        }
        Shift saved = shiftRepository.save(shift);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Shift created successfully", ShiftResponseDTO.fromEntity(saved)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_READ_SHIFT')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<ShiftResponseDTO>> getById(
            @PathVariable UUID id, HttpServletRequest request) {
        Tenant tenant = requireTenant(request);
        Shift shift = findAccessibleShift(id, tenant);
        return ResponseEntity.ok(new ApiResponse<>(true, "Shift fetched successfully", ShiftResponseDTO.fromEntity(shift)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_SHIFT')")
    public ResponseEntity<ApiResponse<ShiftResponseDTO>> update(
            @PathVariable UUID id, @Valid @RequestBody ShiftRequestDTO body, HttpServletRequest request) {
        Tenant tenant = requireTenant(request);
        Shift shift = findAccessibleShift(id, tenant);
        Tenant requestedOrganisation = resolveOrganisation(body, tenant);
        if (!shift.getOrganisation().getId().equals(requestedOrganisation.getId())) {
            throw new RuntimeException("A shift cannot be moved to another organisation");
        }
        shift.setName(body.getName().trim());
        shift.setStartTime(body.getStartTime());
        shift.setEndTime(body.getEndTime());
        shift.setShiftType(body.getShiftType());
        Object updatedBy = request.getAttribute("updatedBy");
        if (updatedBy != null) {
            shift.setUpdatedBy(updatedBy.toString());
        }
        Shift saved = shiftRepository.save(shift);
        return ResponseEntity.ok(new ApiResponse<>(true, "Shift updated successfully", ShiftResponseDTO.fromEntity(saved)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('CAN_READ_SHIFT')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Page<ShiftResponseDTO>>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String organisationId,
            Pageable pageable,
            HttpServletRequest request) {
        Tenant tenant = requireTenant(request);
        Page<Shift> shifts;
        if (tenant.getTenantType() == Tenant.TenantType.ORGANISATION) {
            if (organisationId != null && !organisationId.isBlank()
                    && !tenant.getId().toString().equalsIgnoreCase(organisationId.trim())) {
                throw new RuntimeException("organisationId must match the authenticated organisation tenant");
            }
            shifts = name == null || name.isBlank()
                    ? shiftRepository.findByOrganisation_IdAndIsDeletedFalse(tenant.getId(), pageable)
                    : shiftRepository.findByOrganisation_IdAndNameContainingIgnoreCaseAndIsDeletedFalse(
                            tenant.getId(), name.trim(), pageable);
        } else if (tenant.getTenantType() == Tenant.TenantType.VENDOR) {
            if (organisationId == null || organisationId.isBlank()) {
                shifts = name == null || name.isBlank()
                        ? shiftRepository.findByTenant_IdAndIsDeletedFalse(tenant.getId(), pageable)
                        : shiftRepository.findByTenant_IdAndNameContainingIgnoreCaseAndIsDeletedFalse(
                                tenant.getId(), name.trim(), pageable);
            } else {
                Tenant organisation = findLinkedOrganisation(tenant, organisationId);
                shifts = name == null || name.isBlank()
                        ? shiftRepository.findByTenant_IdAndOrganisation_IdAndIsDeletedFalse(
                                tenant.getId(), organisation.getId(), pageable)
                        : shiftRepository.findByTenant_IdAndOrganisation_IdAndNameContainingIgnoreCaseAndIsDeletedFalse(
                                tenant.getId(), organisation.getId(), name.trim(), pageable);
            }
        } else {
            throw new RuntimeException("Only vendor and organisation tenants can manage shifts");
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Shifts fetched successfully", shifts.map(ShiftResponseDTO::fromEntity)));
    }

    private Tenant resolveOrganisation(ShiftRequestDTO body, Tenant tenant) {
        if (tenant.getTenantType() == Tenant.TenantType.ORGANISATION) {
            if (body.getOrganisationId() != null && !body.getOrganisationId().isBlank()
                    && !tenant.getId().toString().equalsIgnoreCase(body.getOrganisationId().trim())) {
                throw new RuntimeException("organisationId must match the authenticated organisation tenant");
            }
            return tenant;
        }
        if (tenant.getTenantType() == Tenant.TenantType.VENDOR) {
            if (body.getOrganisationId() == null || body.getOrganisationId().isBlank()) {
                throw new RuntimeException("organisationId is required when a vendor creates a shift");
            }
            return findLinkedOrganisation(tenant, body.getOrganisationId());
        }
        throw new RuntimeException("Only vendor and organisation tenants can manage shifts");
    }

    private Tenant findLinkedOrganisation(Tenant vendor, String organisationId) {
        UUID id;
        try {
            id = UUID.fromString(organisationId.trim());
        } catch (IllegalArgumentException exception) {
            throw new RuntimeException("organisationId must be a valid UUID");
        }
        Tenant organisation = tenantRepository.findById(id)
                .filter(item -> item.getTenantType() == Tenant.TenantType.ORGANISATION)
                .orElseThrow(() -> new RuntimeException("Organisation not found"));
        if (vendorOrganisationRepository.findByVendorAndOrganisation_Id(vendor, organisation.getId()).isEmpty()) {
            throw new RuntimeException("Vendor is not linked with this organisation");
        }
        return organisation;
    }

    private Shift findAccessibleShift(UUID id, Tenant tenant) {
        if (tenant.getTenantType() == Tenant.TenantType.ORGANISATION) {
            return shiftRepository.findByIdAndOrganisation_IdAndIsDeletedFalse(id, tenant.getId())
                    .orElseThrow(() -> new RuntimeException("Shift not found"));
        }
        if (tenant.getTenantType() == Tenant.TenantType.VENDOR) {
            return shiftRepository.findByIdAndTenant_IdAndIsDeletedFalse(id, tenant.getId())
                    .orElseThrow(() -> new RuntimeException("Shift not found"));
        }
        throw new RuntimeException("Only vendor and organisation tenants can manage shifts");
    }

    private Tenant requireTenant(HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        if (tenant == null || tenant.getId() == null) {
            throw new RuntimeException("Tenant not found in token");
        }
        return tenant;
    }
}
