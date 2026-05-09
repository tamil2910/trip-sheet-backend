package com.example.trip_sheet_backend.dtos.TripDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.TripFeedback;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TripFeedbackResponseDTO {

    private UUID id;
    private UUID tripId;
    private UUID passengerId;
    private Integer driverBehaviourRating;
    private Integer driverCleanlinessRating;
    private Integer vehicleConditionRating;
    private Integer vehicleCleanlinessRating;
    private Integer overallExperienceRating;
    private UUID executedVendorId;
    private UUID originVendorId;
    private UUID organisationId;

    public static TripFeedbackResponseDTO fromEntity(TripFeedback feedback) {
        if (feedback == null) {
            return null;
        }

        return new TripFeedbackResponseDTO(
                feedback.getId(),
                feedback.getTrip() != null ? feedback.getTrip().getId() : null,
                feedback.getPassenger() != null ? feedback.getPassenger().getId() : null,
                feedback.getDriverBehaviourRating(),
                feedback.getDriverCleanlinessRating(),
                feedback.getVehicleConditionRating(),
                feedback.getVehicleCleanlinessRating(),
                feedback.getOverallExperienceRating(),
                feedback.getExecutedVendorId(),
                feedback.getOriginVendorId(),
                feedback.getOrganisationId());
    }
}