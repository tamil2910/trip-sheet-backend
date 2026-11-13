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
import com.example.trip_sheet_backend.response_setups.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/shifts")
@Transactional
public class ShiftController {
    private final ShiftRepository shiftRepository;

    public ShiftController(ShiftRepository shiftRepository) {
        this.shiftRepository = shiftRepository;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CAN_CREATE_SHIFT')")
    public ResponseEntity<ApiResponse<ShiftResponseDTO>> create(
            @Valid @RequestBody ShiftRequestDTO body, HttpServletRequest request) {
        Tenant tenant = requireTenant(request);
        Shift shift = new Shift();
        shift.setName(body.getName().trim());
        shift.setStartTime(body.getStartTime());
        shift.setEndTime(body.getEndTime());
        shift.setTenant(tenant);
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
        Shift shift = shiftRepository.findByIdAndTenant_IdAndIsDeletedFalse(id, tenant.getId())
                .orElseThrow(() -> new RuntimeException("Shift not found"));
        return ResponseEntity.ok(new ApiResponse<>(true, "Shift fetched successfully", ShiftResponseDTO.fromEntity(shift)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_SHIFT')")
    public ResponseEntity<ApiResponse<ShiftResponseDTO>> update(
            @PathVariable UUID id, @Valid @RequestBody ShiftRequestDTO body, HttpServletRequest request) {
        Tenant tenant = requireTenant(request);
        Shift shift = shiftRepository.findByIdAndTenant_IdAndIsDeletedFalse(id, tenant.getId())
                .orElseThrow(() -> new RuntimeException("Shift not found"));
        shift.setName(body.getName().trim());
        shift.setStartTime(body.getStartTime());
        shift.setEndTime(body.getEndTime());
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
            Pageable pageable,
            HttpServletRequest request) {
        Tenant tenant = requireTenant(request);
        Page<Shift> shifts = name == null || name.isBlank()
                ? shiftRepository.findByTenant_IdAndIsDeletedFalse(tenant.getId(), pageable)
                : shiftRepository.findByTenant_IdAndNameContainingIgnoreCaseAndIsDeletedFalse(
                        tenant.getId(), name.trim(), pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Shifts fetched successfully", shifts.map(ShiftResponseDTO::fromEntity)));
    }

    private Tenant requireTenant(HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        if (tenant == null || tenant.getId() == null) {
            throw new RuntimeException("Tenant not found in token");
        }
        return tenant;
    }
}
