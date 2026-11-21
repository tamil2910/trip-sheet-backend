package com.example.trip_sheet_backend.dtos;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.TransportRoute;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TransportRouteDraftConfirmDTO {
    /** Optional when the selected requests identify one shared shift. */
    private UUID shiftId;

    /** Epoch seconds. */
    @NotNull
    private Long validFrom;

    /** Epoch seconds. */
    @NotNull
    private Long validTo;

    @Valid
    @NotEmpty
    private List<Group> groups;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Group {
        @NotNull
        private TransportRoute.RouteDirection routeDirection;
        private UUID vehicleTypeId;
        @NotNull
        private Integer allowedPassenger;
        private String direction;
        private String pickupAddress;
        private String dropoffAddress;
        /** Optional selected vendor. Omit to save this draft without assigning a vendor. */
        private UUID vendorId;
        @Valid
        @NotEmpty
        private List<TransportRequestShiftItemDTO> requests;
    }
}
