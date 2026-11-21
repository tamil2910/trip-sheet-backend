package com.example.trip_sheet_backend.dtos;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.TransportRoute;
import com.example.trip_sheet_backend.models.Shift;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class TransportRequestRouteGroupDTO {
    private UUID shiftId;
    private String shiftName;
    private String shiftStartTime;
    private String shiftEndTime;
    private Shift.ShiftType shiftType;
    private List<RouteGroup> routes;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RouteGroup {
        private UUID routeId;
        private TransportRoute.RouteDirection routeDirection;
        private TransportRoute.RouteStatus routeStatus;
        private UUID vehicleTypeId;
        private Integer allowedPassenger;
        private String direction;
        private String pickupAddress;
        private String dropoffAddress;
        private List<TransportRequestShiftItemDTO> requests;
        private UUID draftId;
        private String draftStatus;
        private Long expiresAt;
        private Long validFrom;
        private Long validTo;
        private Double totalRouteKm;
        private RouteLengthCategory routeLengthCategory;
        private UUID suggestedVendorId;
        private String suggestedVendorName;
        private Double suggestedVendorAllocationPercentage;
        private UUID assignedVendorId;
        private String assignedVendorName;

        public RouteGroup(UUID routeId, TransportRoute.RouteDirection routeDirection,
                TransportRoute.RouteStatus routeStatus, UUID vehicleTypeId, Integer allowedPassenger,
                String direction, String pickupAddress, String dropoffAddress,
                List<TransportRequestShiftItemDTO> requests) {
            this(routeId, routeDirection, routeStatus, vehicleTypeId, allowedPassenger,
                    direction, pickupAddress, dropoffAddress, requests,
                    null, null, null, null, null, null, null,
                    null, null, null, null, null);
        }
    }

    public enum RouteLengthCategory {
        SHORTEST,
        MEDIUM,
        LONGEST
    }
}
