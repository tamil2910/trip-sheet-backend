package com.example.trip_sheet_backend.dtos.AttendanceDtos;

import java.time.LocalDate;
import java.util.UUID;

import com.example.trip_sheet_backend.models.DriverAttendance;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DriverAttendanceResponseDto {
    private UUID id;
    private UUID driverId;
    private LocalDate attendanceDate;
    private Long clockInAt;
    private Long clockOutAt;
    private Long workedDurationMinutes;

    public DriverAttendanceResponseDto(DriverAttendance attendance) {
        id = attendance.getId();
        driverId = attendance.getDriver().getId();
        attendanceDate = attendance.getAttendanceDate();
        clockInAt = attendance.getClockInAt();
        clockOutAt = attendance.getClockOutAt();
        workedDurationMinutes = attendance.getWorkedDurationMinutes();
    }
}
