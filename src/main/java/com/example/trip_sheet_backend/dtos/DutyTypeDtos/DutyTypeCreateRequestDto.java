package com.example.trip_sheet_backend.dtos.DutyTypeDtos;

import com.example.trip_sheet_backend.models.DutyType;

import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DutyTypeCreateRequestDto {

    @Nullable
    @Min(value = 0, message = "KM cannot be negative")
    private Integer km;

    @Nullable
    @Min(value = 0, message = "HR cannot be negative")
    private Integer hr;

    @Nullable
    @Min(value = 1, message = "Max hours per day must be at least 1")
    private Integer maxHrPerDay;

    @Nullable
    @Min(value = 0, message = "Total hours must be 0 or above")
    private Integer totalHr;

    @Nullable
    @Min(value = 0, message = "Total KM must be 0 or above")
    private Integer totalKm;

    @Nullable
    @Min(value = 1, message = "Max days must be at least 1")
    private Integer maxDays;

    // @NotBlank(message = "Custom Duty type name is required")
    private String custom_name;

    @NotNull(message = "Type of duty is required")
    private DutyType.typeDuty typeOfDuty;

    @Nullable
    private DutyType.TypeAirportTransfer airportTransferType;

    private Boolean isGlobal = false;

    // @NotNull(message = "Tenant is required!")
    private String tenant_id;

}
