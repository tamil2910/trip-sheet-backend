package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.PassengerRouteDtos;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.PassengerRouteService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/passengers/route-groups")
@PreAuthorize("isAuthenticated()")
public class PassengerRouteController {

    private final PassengerRouteService passengerRouteService;

    public PassengerRouteController(PassengerRouteService passengerRouteService) {
        this.passengerRouteService = passengerRouteService;
    }

    @PostMapping("/analyse")
    public ResponseEntity<ApiResponse<PassengerRouteDtos.Response>> analyse(
            @RequestBody PassengerRouteDtos.AnalyseRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Passenger routes analysed successfully",
                passengerRouteService.analyse(request)));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PassengerRouteDtos.Response>> confirm(
            HttpServletRequest httpRequest,
            @RequestBody PassengerRouteDtos.ConfirmationRequest request) {
        Tenant organisation = (Tenant) httpRequest.getAttribute("tenant");
        UUID createdBy = (UUID) httpRequest.getAttribute("createdBy");
        return ResponseEntity.ok(new ApiResponse<>(true, "Passenger route groups confirmed successfully",
                passengerRouteService.confirm(request, organisation, createdBy)));
    }
}