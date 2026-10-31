package com.example.trip_sheet_backend.dtos.AttendanceDtos;

import java.time.LocalDate;
import java.util.UUID;

import com.example.trip_sheet_backend.models.VendorEmployeeAttendance;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttendanceResponseDto {
    private UUID id;
    private UUID vendorEmployeeId;
    private LocalDate attendanceDate;
    private Long clockInAt;
    private Long clockOutAt;
    private Long workedDurationMinutes;

    public AttendanceResponseDto(VendorEmployeeAttendance attendance) {
        this.id = attendance.getId();
        this.vendorEmployeeId = attendance.getVendorEmployee().getId();
        this.attendanceDate = attendance.getAttendanceDate();
        this.clockInAt = attendance.getClockInAt();
        this.clockOutAt = attendance.getClockOutAt();
        this.workedDurationMinutes = attendance.getWorkedDurationMinutes();
    }
}
