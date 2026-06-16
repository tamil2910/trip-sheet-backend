package com.example.trip_sheet_backend.controllers;

import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripResponseDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripUpdateRequestDTO;
import com.example.trip_sheet_backend.mappers.TripResponseMapper;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.PassengerService.PassengerService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/passengers")
@PreAuthorize("hasRole('GUEST') or hasRole('ADMIN')")
public class PassengerController {

    private final PassengerService passengerService;

    public PassengerController(PassengerService passengerService) {
        this.passengerService = passengerService;
    }

    @GetMapping("/organisation/search")
    public ResponseEntity<ApiResponse<Tenant>> searchOrganisation(@RequestParam String uniqueCode) {
        Tenant org = passengerService.searchOrganisationByCode(uniqueCode);
        return ResponseEntity.ok(new ApiResponse<>(true, "Organisation found", org));
    }

    @PostMapping("/organisation/link")
    public ResponseEntity<ApiResponse<Void>> linkOrganisation(HttpServletRequest request, @RequestBody Map<String, String> body) {
        UserAccount user = (UserAccount) request.getAttribute("user");
        String uniqueCode = body == null ? null : body.get("uniqueCode");
        passengerService.linkOrganisation(user, uniqueCode);
        return ResponseEntity.ok(new ApiResponse<>(true, "Organisation linked successfully", null));
    }

    @PostMapping("/trips")
    public ResponseEntity<ApiResponse<TripResponseDTO>> createTrip(HttpServletRequest request, @RequestBody TripCreateRequestDTO dto) {
        UserAccount user = (UserAccount) request.getAttribute("user");
        Trip trip = passengerService.createPassengerTrip(dto, user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Trip request created", TripResponseMapper.toDTO(trip)));
    }

    @GetMapping("/trips")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTrips(HttpServletRequest request, @RequestParam Map<String, Object> filters, Pageable pageable) {
        UserAccount user = (UserAccount) request.getAttribute("user");
        Page<Trip> trips = passengerService.getMyTrips(user, filters, pageable);
        var data = trips.map(TripResponseMapper::toDTO).getContent();

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("data", data);
        response.put("currentPage", trips.getNumber());
        response.put("pageSize", trips.getSize());
        response.put("currentPageCount", trips.getNumberOfElements());
        response.put("totalItems", trips.getTotalElements());
        response.put("totalPages", trips.getTotalPages());
        response.put("isFirst", trips.isFirst());
        response.put("isLast", trips.isLast());
        response.put("hasNext", trips.hasNext());
        response.put("hasPrevious", trips.hasPrevious());
        response.put("page", pageable != null ? pageable.getPageNumber() : trips.getNumber());
        response.put("size", pageable != null ? pageable.getPageSize() : trips.getSize());

        return ResponseEntity.ok(new ApiResponse<>(true, "Trips fetched", response));
    }

    @GetMapping("/trips/{id}")
    public ResponseEntity<ApiResponse<TripResponseDTO>> getTrip(HttpServletRequest request, @PathVariable UUID id) {
        UserAccount user = (UserAccount) request.getAttribute("user");
        Trip trip = passengerService.getTripDetails(id, user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Trip details fetched", TripResponseMapper.toDTO(trip)));
    }

    @PutMapping("/trips/{id}")
    public ResponseEntity<ApiResponse<TripResponseDTO>> updateTrip(HttpServletRequest request, @PathVariable UUID id, @RequestBody TripUpdateRequestDTO dto) {
        UserAccount user = (UserAccount) request.getAttribute("user");
        Trip trip = passengerService.updateMyTrip(id, dto, user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Trip updated successfully", TripResponseMapper.toDTO(trip)));
    }

    @DeleteMapping("/trips/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTrip(HttpServletRequest request, @PathVariable UUID id) {
        UserAccount user = (UserAccount) request.getAttribute("user");
        passengerService.deleteMyTrip(id, user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Trip cancelled successfully", null));
    }
}
