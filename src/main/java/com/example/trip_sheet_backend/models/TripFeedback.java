package com.example.trip_sheet_backend.models;

import java.util.UUID;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "trip_feedbacks",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"trip_id", "passenger_id"})
    },
    indexes = {
        @Index(name = "idx_trip_feedback_trip", columnList = "trip_id"),
        @Index(name = "idx_trip_feedback_passenger", columnList = "passenger_id"),
        @Index(name = "idx_trip_feedback_org", columnList = "organisation_id")
    }
)
public class TripFeedback extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private PeopleTenant passenger;

    @Column(name = "driver_behaviour_rating", nullable = false)
    private Integer driverBehaviourRating;

    @Column(name = "driver_cleanliness_rating", nullable = false)
    private Integer driverCleanlinessRating;

    @Column(name = "vehicle_condition_rating", nullable = false)
    private Integer vehicleConditionRating;

    @Column(name = "vehicle_cleanliness_rating", nullable = false)
    private Integer vehicleCleanlinessRating;

    @Column(name = "overall_experience_rating", nullable = false)
    private Integer overallExperienceRating;

    @Column(name = "executed_vendor_id", nullable = false)
    private UUID executedVendorId;

    @Column(name = "origin_vendor_id", nullable = false)
    private UUID originVendorId;

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;
}