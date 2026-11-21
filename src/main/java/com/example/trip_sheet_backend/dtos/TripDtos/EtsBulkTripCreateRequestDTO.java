package com.example.trip_sheet_backend.dtos.TripDtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EtsBulkTripCreateRequestDTO {
    /** Default duty type for trip items that do not specify their own. */
    private String dutyTypeId;

    /** Optional default vendor for trip items; each trip may override it. */
    private String vendorId;

    @Valid
    @NotEmpty(message = "At least one ETS trip is required")
    private List<EtsTripCreateRequestDTO> trips;
}
