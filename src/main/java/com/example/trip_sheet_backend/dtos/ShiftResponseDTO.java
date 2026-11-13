package com.example.trip_sheet_backend.dtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.Shift;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ShiftResponseDTO {
    private UUID id;
    private String name;
    private String startTime;
    private String endTime;
    private UUID tenantId;

    public static ShiftResponseDTO fromEntity(Shift shift) {
        return new ShiftResponseDTO(shift.getId(), shift.getName(), shift.getStartTime(), shift.getEndTime(),
                shift.getTenant() == null ? null : shift.getTenant().getId());
    }
}
