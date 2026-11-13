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
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transport_requests")
public class TransportRequest extends BaseModel implements TenantScoped {

    @NotNull(message = "Tenant is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Organisation receiving the transport request; differs from tenant when a vendor submits it. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private Tenant organisation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private TransportRoute route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id")
    private UserAccount requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id")
    private PeopleTenant passenger;

    @Enumerated(EnumType.STRING)
    @Column(name = "passenger_type")
    private PassengerType passengerType;

    @Column(name = "passenger_name")
    private String passengerName;

    @Column(name = "passenger_email")
    private String passengerEmail;

    @Column(name = "passenger_phone")
    private String passengerPhone;

    @NotNull(message = "Trip type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type", nullable = false)
    private Trip.TripType tripType;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private Long startDate;

    /** Pickup datetime in epoch seconds, calculated from the request date and selected shift start time. */
    @Column(name = "pickup_time")
    private Long pickupTime;

    @Column(name = "end_date")
    private Long endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_frequency")
    private Trip.RecurrenceFrequency recurrenceFrequency;

    @Column(name = "recurrence_interval")
    private Integer recurrenceInterval = 1;

    /** Comma-separated day codes, for example MON,WED,FRI. */
    @Column(name = "days_of_week")
    private String daysOfWeek;

    @NotNull(message = "Pickup address is required")
    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @NotNull(message = "Pickup latitude is required")
    @Column(name = "pickup_latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    @Column(name = "pickup_longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal pickupLongitude;

    @NotNull(message = "Drop-off address is required")
    @Column(name = "dropoff_address", nullable = false)
    private String dropoffAddress;

    @NotNull(message = "Drop-off latitude is required")
    @Column(name = "dropoff_latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal dropoffLatitude;

    @NotNull(message = "Drop-off longitude is required")
    @Column(name = "dropoff_longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal dropoffLongitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    public enum PassengerType {
        EMPLOYEE,
        VISITOR
    }

    public enum RequestStatus {
        PENDING,
        APPROVED,
        REJECTED,
        CANCELLED
    }
}
