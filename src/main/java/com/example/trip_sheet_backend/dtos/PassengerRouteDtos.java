package com.example.trip_sheet_backend.dtos;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public final class PassengerRouteDtos {

    private PassengerRouteDtos() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AnalyseRequest {
        private String organisationId;
        private String companyName;
        private Instant arrivalTime;
        private Boolean isEstimatedHrKm;
        private List<Passenger> passengers = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ConfirmationRequest {
        private String organisationId;
        private String companyName;
        private Instant arrivalTime;
        private List<Group> groups = new ArrayList<>();
        private List<Passenger> unmatchedPassengers = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Passenger {
        private UUID passengerId;
        private String name;
        private String phone;
        private String email;
        private String reportingAddress;
        private String dropAddress;
        private Instant pickupTime;
        private Instant arrivalTime;
        private String location;
        private String city;
        private Double latitude;
        private Double longitude;
        private Double destinationLatitude;
        private Double destinationLongitude;
        private Double estimatedKmToPickup;
        private Long estimatedMinutesToPickup;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Group {
        private String groupId;
        private String companyName;
        private String destination;
        private Instant startPickupTime;
        private Instant estimatedArrivalTime;
        private Double estimatedTotalKm;
        private Long estimatedTotalMinutes;
        private String routeStatus;
        private List<Passenger> passengers = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Response {
        private Instant arrivalTime;
        private UUID organisationId;
        private String name;
        private List<OrganisationMatch> organisationMatches = new ArrayList<>();
        private List<Group> groups = new ArrayList<>();
        private List<Passenger> unmatchedPassengers = new ArrayList<>();

        public Response(Instant arrivalTime) {
            this.arrivalTime = arrivalTime;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class OrganisationMatch {
        private UUID organisationId;
        private String name;

        public OrganisationMatch(UUID organisationId, String name) {
            this.organisationId = organisationId;
            this.name = name;
        }
    }
}