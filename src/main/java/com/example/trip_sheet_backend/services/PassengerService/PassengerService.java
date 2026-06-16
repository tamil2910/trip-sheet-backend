package com.example.trip_sheet_backend.services.PassengerService;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.UserAccount;

public interface PassengerService {
    Tenant searchOrganisationByCode(String uniqueCode);
    void linkOrganisation(UserAccount user, String uniqueCode);
    Trip createPassengerTrip(TripCreateRequestDTO dto, UserAccount user);
    Page<Trip> getMyTrips(UserAccount user, Map<String, Object> filters, Pageable pageable);
    Trip updateMyTrip(UUID tripId, TripUpdateRequestDTO dto, UserAccount user);
    void deleteMyTrip(UUID tripId, UserAccount user);
    Trip getTripDetails(UUID tripId, UserAccount user);
}
