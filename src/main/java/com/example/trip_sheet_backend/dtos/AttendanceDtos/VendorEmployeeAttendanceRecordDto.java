package com.example.trip_sheet_backend.dtos.AttendanceDtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.example.trip_sheet_backend.models.VendorEmployeeAttendance.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VendorEmployeeAttendanceRecordDto {
    private UUID employeeId;

    @NotNull
    private LocalDate date;

    @NotNull
    private AttendanceStatus status;

    private LocalTime checkIn;
    private LocalTime checkOut;
    private Long workedMinutes;
}
