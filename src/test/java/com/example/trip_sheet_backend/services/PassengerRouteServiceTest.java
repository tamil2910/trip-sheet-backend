package com.example.trip_sheet_backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.trip_sheet_backend.dtos.PassengerRouteDtos;
import com.example.trip_sheet_backend.repositories.PeopleTenantRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.services.PeopleTenantService.PeopleTenantServiceImp;

class PassengerRouteServiceTest {

    private PassengerRouteEstimateService estimateService;
    private PassengerRouteService routeService;

    @BeforeEach
    void setUp() {
        estimateService = mock(PassengerRouteEstimateService.class);
        routeService = new PassengerRouteService(
                mock(PeopleTenantRepository.class),
                mock(PeopleTenantServiceImp.class),
                mock(PassengerGeocodingService.class),
                mock(TenantRepository.class),
                estimateService);
    }

    @Test
    void doesNotRequestRouteEstimatesWhenFlagIsMissingOrFalse() {
        PassengerRouteDtos.AnalyseRequest request = request(false);

        PassengerRouteDtos.Response response = routeService.analyse(request);

        assertEquals(1, response.getGroups().size());
        assertEquals(null, response.getGroups().get(0).getEstimatedTotalKm());
        verifyNoInteractions(estimateService);
    }

    @Test
    void returnsLegAndGroupEstimatesWhenFlagIsTrue() {
        PassengerRouteDtos.AnalyseRequest request = request(true);
        when(estimateService.estimate(anyList(), any())).thenReturn(
                new PassengerRouteEstimateService.RouteEstimate(
                        17.0,
                        35,
                        List.of(
                                new PassengerRouteEstimateService.RouteLeg(5.0, 10),
                                new PassengerRouteEstimateService.RouteLeg(12.0, 25))));

        PassengerRouteDtos.Response response = routeService.analyse(request);

        PassengerRouteDtos.Group group = response.getGroups().get(0);
        assertEquals(17.0, group.getEstimatedTotalKm());
        assertEquals(35L, group.getEstimatedTotalMinutes());
        assertEquals(0.0, group.getPassengers().get(0).getEstimatedKmToPickup());
        assertEquals(5.0, group.getPassengers().get(1).getEstimatedKmToPickup());
        assertEquals(10L, group.getPassengers().get(1).getEstimatedMinutesToPickup());
        verify(estimateService).estimate(anyList(), any());
    }

    private PassengerRouteDtos.AnalyseRequest request(boolean estimate) {
        PassengerRouteDtos.AnalyseRequest request = new PassengerRouteDtos.AnalyseRequest();
        request.setArrivalTime(Instant.parse("2026-09-29T09:00:00Z"));
        request.setIsEstimatedHrKm(estimate);
        request.setPassengers(List.of(
                passenger("First", "2026-09-29T06:00:00Z", 12.9000, 77.6000),
                passenger("Second", "2026-09-29T06:15:00Z", 12.9100, 77.6100)));
        return request;
    }

    private PassengerRouteDtos.Passenger passenger(String name, String pickupTime, double latitude, double longitude) {
        PassengerRouteDtos.Passenger passenger = new PassengerRouteDtos.Passenger();
        passenger.setName(name);
        passenger.setPickupTime(Instant.parse(pickupTime));
        passenger.setLocation("Bengaluru");
        passenger.setCity("Bengaluru");
        passenger.setDropAddress("Central Station");
        passenger.setLatitude(latitude);
        passenger.setLongitude(longitude);
        passenger.setDestinationLatitude(13.0000);
        passenger.setDestinationLongitude(77.6000);
        return passenger;
    }
}