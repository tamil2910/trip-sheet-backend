package com.example.trip_sheet_backend.models;

import java.time.LocalDate;

import com.example.trip_sheet_backend.common.models.BaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "driver_attendance",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_driver_attendance_day",
        columnNames = {"driver_id", "attendance_date"}
    ),
    indexes = @Index(name = "idx_driver_attendance_tenant_day", columnList = "tenant_id, attendance_date")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverAttendance extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", length = 16)
    private AttendanceStatus status;

    public enum AttendanceStatus {
        OFF, ABSENT, PRESENT, HALFDAY
    }

    /** Unix epoch milliseconds. */
    @Column(name = "clock_in_at")
    private Long clockInAt;

    /** Unix epoch milliseconds. */
    @Column(name = "clock_out_at")
    private Long clockOutAt;

    /** Elapsed duty time, rounded down to whole minutes. */
    @Column(name = "worked_duration_minutes")
    private Long workedDurationMinutes;
}
