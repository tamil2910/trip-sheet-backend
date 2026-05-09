package com.example.trip_sheet_backend.dtos.TripDtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripFeedbackRequestDTO {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer driverBehaviourRating;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer driverCleanlinessRating;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer vehicleConditionRating;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer vehicleCleanlinessRating;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer overallExperienceRating;
}