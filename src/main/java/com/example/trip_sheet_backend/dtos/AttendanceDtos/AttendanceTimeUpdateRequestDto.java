package com.example.trip_sheet_backend.dtos.AttendanceDtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AttendanceTimeUpdateRequestDto {
    private Long clockInAt;
    private Long clockOutAt;
}
