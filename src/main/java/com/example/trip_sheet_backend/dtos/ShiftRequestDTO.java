package com.example.trip_sheet_backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.example.trip_sheet_backend.models.Shift.ShiftType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShiftRequestDTO {
    /** Required for vendor-created shifts; organisation users default to their own tenant. */
    private String organisationId;

    @NotBlank(message = "Shift name is required")
    private String name;

    @NotNull(message = "Shift start time is required")
    private String startTime;

    @NotNull(message = "Shift end time is required")
    private String endTime;

    @NotNull(message = "Shift type is required")
    private ShiftType shiftType;
}
