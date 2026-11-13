package com.example.trip_sheet_backend.dtos;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.TransportRequest;
import com.example.trip_sheet_backend.models.Shift;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransportRequestShiftGroupDTO {
    private UUID shiftId;
    private String shiftName;
    private String startTime;
    private String endTime;
    private Shift.ShiftType shiftType;
    private List<TransportRequestShiftItemDTO> requests;

    public static TransportRequestShiftGroupDTO fromShiftAndRequests(
            TransportRequest request, List<TransportRequest> requests) {
        return new TransportRequestShiftGroupDTO(
                request.getShift() == null ? null : request.getShift().getId(),
                request.getShift() == null ? null : request.getShift().getName(),
                request.getShift() == null ? null : request.getShift().getStartTime(),
                request.getShift() == null ? null : request.getShift().getEndTime(),
                request.getShift() == null ? null : request.getShift().getShiftType(),
                requests.stream().map(TransportRequestShiftItemDTO::fromEntity).toList());
    }
}
