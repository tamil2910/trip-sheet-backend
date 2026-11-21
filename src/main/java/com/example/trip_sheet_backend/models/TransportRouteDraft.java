package com.example.trip_sheet_backend.models;

import java.util.UUID;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transport_route_drafts")
@Getter
@Setter
@NoArgsConstructor
public class TransportRouteDraft extends BaseModel {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Tenant organisation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Tenant assignedVendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_direction", nullable = false)
    private TransportRoute.RouteDirection routeDirection;

    @Column(name = "vehicle_type_id")
    private UUID vehicleTypeId;

    @Column(name = "allowed_passenger", nullable = false)
    private Integer allowedPassenger;

    @Column(name = "direction_text", nullable = false, length = 1000)
    private String direction;

    @Column(name = "pickup_address", nullable = false, length = 500)
    private String pickupAddress;

    @Column(name = "dropoff_address", nullable = false, length = 500)
    private String dropoffAddress;

    @Column(name = "requests_json", nullable = false, columnDefinition = "json")
    private String requestsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "draft_status", nullable = false)
    private DraftStatus draftStatus = DraftStatus.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    private Long expiresAt;

    @Column(name = "valid_from")
    private Long validFrom;

    @Column(name = "valid_to")
    private Long validTo;

    @Column(name = "total_route_km")
    private Double totalRouteKm;

    @Column(name = "route_length_category", length = 16)
    private String routeLengthCategory;

    public enum DraftStatus {
        ACTIVE,
        EXPIRED
    }
}
