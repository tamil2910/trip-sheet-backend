package com.example.trip_sheet_backend.dtos;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.TransportRoute;
import com.example.trip_sheet_backend.models.Shift;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
    @AllArgsConstructor
    public static class RouteGroup {
        private UUID routeId;
        private TransportRoute.RouteDirection routeDirection;
        private String direction;
        private String pickupAddress;
        private String dropoffAddress;
        private List<TransportRequestShiftItemDTO> requests;
    }
}
