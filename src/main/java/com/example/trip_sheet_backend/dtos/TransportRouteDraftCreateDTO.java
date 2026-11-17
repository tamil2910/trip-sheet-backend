package com.example.trip_sheet_backend.dtos;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.TransportRoute;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TransportRouteDraftCreateDTO {
    @NotEmpty(message = "At least one request is required")
    private List<UUID> requestIds;

    @NotNull(message = "Route direction is required")
    private TransportRoute.RouteDirection routeDirection;
}
