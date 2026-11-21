package com.example.trip_sheet_backend.controllers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.trip_sheet_backend.dtos.TripDtos.EtsTripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripStopPassengerRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.TripStop;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;

@Component
public class EtsTripRequestValidator {
    private final VendorOrganisationRepository vendorOrganisationRepository;

    public EtsTripRequestValidator(VendorOrganisationRepository vendorOrganisationRepository) {
        this.vendorOrganisationRepository = vendorOrganisationRepository;
    }

    public void prepare(EtsTripCreateRequestDTO body, Tenant tenant) {
        if (body.getAllowedPassenger() == null || body.getAllowedPassenger() < 1) {
            throw new IllegalArgumentException("allowedPassenger must be greater than zero");
        }
        if (body.getPassengerIds() == null || body.getPassengerIds().isEmpty()) {
            throw new IllegalArgumentException("passengerIds from the route group are required");
        }
        if (body.getPassengerIds().stream().distinct().count() != body.getPassengerIds().size()) {
            throw new IllegalArgumentException("passengerIds must not contain duplicates");
        }
        if (body.getPassengerIds().size() > body.getAllowedPassenger()) {
            throw new IllegalArgumentException("Passenger count exceeds allowedPassenger for the selected vehicle type");
        }
        if (body.getStops() == null || body.getStops().isEmpty()) {
            throw new IllegalArgumentException("stops from the route group are required");
        }
        validatePickupAssignments(body);
        applyTenantRelations(body, tenant);
        body.setTripCategory(Trip.TripCategory.ETS);
        body.setDriverId(null);
        body.setVehicleId(null);
    }

    private void validatePickupAssignments(EtsTripCreateRequestDTO body) {
        Set<String> tripPassengers = new HashSet<>(body.getPassengerIds());
        Set<String> assignedPassengers = new HashSet<>();
        for (var stop : body.getStops()) {
            if (stop.getStopType() != TripStop.StopType.PICKUP) continue;
            if (stop.getPassengers() == null || stop.getPassengers().isEmpty()) {
                throw new IllegalArgumentException("Every ETS pickup stop must list its passengers and pickup times");
            }
            for (TripStopPassengerRequestDTO passenger : stop.getPassengers()) {
                if (passenger == null || passenger.getPassengerId() == null || passenger.getPickupTime() == null) {
                    throw new IllegalArgumentException("Every ETS pickup passenger requires passengerId and pickupTime");
                }
                if (!tripPassengers.contains(passenger.getPassengerId())) {
                    throw new IllegalArgumentException("Pickup stop passenger must be included in passengerIds");
                }
                if (!assignedPassengers.add(passenger.getPassengerId())) {
                    throw new IllegalArgumentException("Each ETS passenger must have exactly one pickup stop");
                }
            }
        }
        if (!assignedPassengers.equals(tripPassengers)) {
            throw new IllegalArgumentException("Each ETS passenger must be assigned to a pickup stop with a pickupTime");
        }
    }

    private void applyTenantRelations(EtsTripCreateRequestDTO body, Tenant tenant) {
        if (tenant.getTenantType() == Tenant.TenantType.ORGANISATION) {
            String vendorId = body.getVendorId();
            body.setOrganisationId(tenant.getId().toString());
            body.setAssociateCustomerId(null);
            if (vendorId != null && !vendorId.isBlank()) {
                UUID selectedVendorId = UUID.fromString(vendorId);
                VendorOrganisation link = vendorOrganisationRepository
                        .findByOrganisationAndVendor_Id(tenant, selectedVendorId)
                        .filter(this::isActiveVendorOrganisation)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "vendorId is not an active vendor relationship for this organisation"));
                body.setVendorId(link.getVendor().getId().toString());
            } else {
                body.setVendorId(null);
            }
            return;
        }

        if (tenant.getTenantType() == Tenant.TenantType.VENDOR) {
            if (body.getOrganisationId() == null || body.getOrganisationId().isBlank()) {
                throw new IllegalArgumentException("organisationId is required when a vendor creates an ETS trip");
            }
            UUID organisationId = UUID.fromString(body.getOrganisationId());
            VendorOrganisation link = vendorOrganisationRepository
                    .findByVendorAndOrganisation_Id(tenant, organisationId)
                    .filter(this::isActiveVendorOrganisation)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "organisationId is not linked to this vendor through an active vendor-organisation relationship"));
            body.setOrganisationId(link.getOrganisation().getId().toString());
            body.setVendorId(tenant.getId().toString());
            body.setAssociateCustomerId(null);
            return;
        }

        throw new IllegalArgumentException("Only organisation and vendor tenants can create ETS trips");
    }

    private boolean isActiveVendorOrganisation(VendorOrganisation link) {
        // ETS trip creation only requires an existing, non-deleted relationship.
        // Contract/approval state is enforced later by billing, not trip creation.
        return link != null
                && !Boolean.TRUE.equals(link.getIsDeleted())
                && link.getVendor() != null
                && link.getOrganisation() != null;
    }
}
