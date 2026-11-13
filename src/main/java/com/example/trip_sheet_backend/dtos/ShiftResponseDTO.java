package com.example.trip_sheet_backend.dtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.Shift;
import com.example.trip_sheet_backend.models.Shift.ShiftType;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ShiftResponseDTO {
    private UUID id;
    private String name;
    private String startTime;
    private String endTime;
    private ShiftType shiftType;
    private UUID tenantId;
    private UUID organisationId;

    public static ShiftResponseDTO fromEntity(Shift shift) {
        return new ShiftResponseDTO(shift.getId(), shift.getName(), shift.getStartTime(), shift.getEndTime(), shift.getShiftType(),
                shift.getTenant() == null ? null : shift.getTenant().getId(),
                shift.getOrganisation() == null ? null : shift.getOrganisation().getId());
    }
}
