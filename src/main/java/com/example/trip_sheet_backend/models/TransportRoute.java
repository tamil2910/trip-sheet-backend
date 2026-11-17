package com.example.trip_sheet_backend.models;

import java.math.BigDecimal;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transport_routes")
public class TransportRoute extends BaseModel implements TenantScoped {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Tenant organisation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_direction", nullable = false)
    private RouteDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_status", nullable = false)
    private RouteStatus status = RouteStatus.ACTIVE;

    /** Anchor route from the first request grouped into this route. */
    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "pickup_latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal pickupLatitude;

    @Column(name = "pickup_longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal pickupLongitude;

    @Column(name = "dropoff_address", nullable = false)
    private String dropoffAddress;

    @Column(name = "dropoff_latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal dropoffLatitude;

    @Column(name = "dropoff_longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal dropoffLongitude;

    public enum RouteDirection {
        UNKNOWN,
        HOME_TO_OFFICE,
        OFFICE_TO_HOME
    }

    public enum RouteStatus {
        DRAFT,
        ACTIVE
    }

    @Override
    public Tenant getTenant() {
        return organisation;
    }

    @Override
    public void setTenant(Tenant tenant) {
        this.organisation = tenant;
    }
}
