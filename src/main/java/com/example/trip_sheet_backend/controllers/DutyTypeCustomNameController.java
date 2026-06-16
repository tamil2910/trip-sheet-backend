package com.example.trip_sheet_backend.controllers;

import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.dtos.DutyTypeDtos.DutyTypeCreateRequestDto;
import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.models.DutyTypeCustomName;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.DutyType.typeDuty;
import com.example.trip_sheet_backend.repositories.DutyTypeCustomNamesRepository;
import com.example.trip_sheet_backend.repositories.DutyTypeRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.DutyTypeCustomNamesService.DutyTypeCustomNamesService;
import com.example.trip_sheet_backend.services.DutyTypeService.DutyTypeService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/custom_duty_type")
public class DutyTypeCustomNameController extends BaseController<DutyTypeCustomName, UUID> {

    private final DutyTypeCustomNamesService service;
    private final DutyTypeService dutyTypeservice;
    private final DutyTypeCustomNamesRepository customNamesRepository;
    private ModelMapper mapper;

    public DutyTypeCustomNameController(DutyTypeCustomNamesService service, ModelMapper mapper,
            DutyTypeService dutyTypeservice, DutyTypeRepository dutyTypeRepository,
            DutyTypeCustomNamesRepository customNamesRepository) {
        super(service);
        this.service = service;
        this.dutyTypeservice = dutyTypeservice;
        this.customNamesRepository = customNamesRepository;
        this.mapper = mapper;
    }

    @PreAuthorize("hasAuthority('CAN_CREATE_DUTYTYPECUSTOMNAME')")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<DutyTypeCustomName>> create_duty_type(
            @Valid @RequestBody DutyTypeCreateRequestDto body,
            HttpServletRequest request) {

        UUID createdBy = (UUID) request.getAttribute("createdBy");
        UUID tenantId = (UUID) request.getAttribute("tenantId");
        Tenant tenant = (Tenant) request.getAttribute("tenant");

        // ---------- BASIC VALIDATION ----------
        if (body.getTypeOfDuty() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "type_of_duty is required", null));
        }

        if (body.getCustom_name() == null || body.getCustom_name().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Custom duty name is required", null));
        }

        typeDuty dutyType = body.getTypeOfDuty();

        DutyType payload = mapper.map(body, DutyType.class);
        payload.setTypeOfDuty(dutyType);
        payload.setCreatedBy(createdBy.toString());

        DutyType savedDutyType;

        // ---------- DUTY TYPE CREATION / REUSE ----------
        switch (dutyType) {

            case LOCAL: {
                if (body.getKm() == null || body.getHr() == null) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse<>(false, "KM & HR required for LOCAL", null));
                }

                String name = body.getHr() + "hr_" + body.getKm() + "km";

                savedDutyType = dutyTypeservice
                        .findLocalDutyType(body.getKm(), body.getHr(), dutyType, name)
                        .orElseGet(() -> {
                            payload.setName(name);
                            return dutyTypeservice.create(payload);
                        });
                break;
            }

            case OUTSTATION: {
                if (body.getKm() == null) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse<>(false, "KM required for OUTSTATION", null));
                }

                String name = "outstation_" + body.getKm() + "km_" +
                        (body.getHr() != null ? body.getHr() + "hr" : "24hr");

                savedDutyType = dutyTypeservice
                        .findOutstation(body.getKm(), dutyType, name)
                        .orElseGet(() -> {
                            payload.setName(name);
                            return dutyTypeservice.create(payload);
                        });
                break;
            }

            case AIRPORT_TRANSFER_FIXED: {
                if (body.getAirportTransferType() == null) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse<>(false, "Airport transfer type required", null));
                }

                String name = "airport_fixed_" + body.getAirportTransferType();

                savedDutyType = dutyTypeservice
                        .findAirportFixed(body.getAirportTransferType())
                        .orElseGet(() -> {
                            payload.setName(name);
                            return dutyTypeservice.create(payload);
                        });
                break;
            }

            case AIRPORT_TRANSFER_KM: {
                if (body.getKm() == null) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse<>(false, "KM required for AIRPORT KM", null));
                }

                String name = "airport_km_" + body.getKm();

                savedDutyType = dutyTypeservice
                        .findAirportKm(body.getKm())
                        .orElseGet(() -> {
                            payload.setName(name);
                            return dutyTypeservice.create(payload);
                        });
                break;
            }

            case MONTHLY_BOOKING_MAX_HR: {
                if (body.getTotalKm() == null) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse<>(false, "Total KM required", null));
                }

                String name = "monthly_max_hr_" + body.getTotalKm() + "km_" +
                        (body.getMaxHrPerDay() != null ? body.getMaxHrPerDay() + "hr" : "12hr") +
                        (body.getMaxDays() != null ? body.getMaxDays() + "days" : "28days");

                savedDutyType = dutyTypeservice
                        .findMonthlyMaxHr(body.getTotalKm(), body.getMaxHrPerDay(), body.getMaxDays())
                        .orElseGet(() -> {
                            payload.setName(name);
                            return dutyTypeservice.create(payload);
                        });
                break;
            }

            case MONTHLY_BOOKING_TOTAL_HR: {
                if (body.getTotalKm() == null || body.getTotalHr() == null || body.getMaxDays() == null) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse<>(false,
                                    "totalKm, totalHr & maxDays required", null));
                }

                String name = "monthly_total_hr_" + body.getTotalKm() + "km_" +
                        body.getTotalHr() + "hr_" + body.getMaxDays() + "days";

                savedDutyType = dutyTypeservice
                        .findMonthlyTotalHr(body.getTotalKm(), body.getTotalHr(), body.getMaxDays())
                        .orElseGet(() -> {
                            payload.setName(name);
                            return dutyTypeservice.create(payload);
                        });
                break;
            }

            case PICKUP_DROP: {
                if (body.getKm() == null) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse<>(false, "KM required for PICKUP_DROP", null));
                }

                String name = body.getKm() + "km";

                payload.setName(name);
                savedDutyType = dutyTypeservice.create(payload);
                break;
            }

            default:
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Unhandled duty type", null));
        }

        // ---------- CUSTOM DUTY NAME ----------
        Optional<DutyTypeCustomName> customExists =
                customNamesRepository.findByDutyTypeIdAndTenantId(
                        savedDutyType.getId(), tenantId);

        if (customExists.isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false,
                            "Custom duty name already exists for this tenant", null));
        }

        DutyTypeCustomName custom = new DutyTypeCustomName();
        custom.setCustomName(body.getCustom_name());
        custom.setDutyType(savedDutyType);
        custom.setTenant(tenant);
        custom.setCreatedBy(createdBy.toString());

        DutyTypeCustomName savedCustom =
                service.createResource(tenantId, custom);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true,
                        "Duty type + custom duty type created successfully",
                        savedCustom));
    }

}
