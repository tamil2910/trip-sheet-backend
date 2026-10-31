package com.example.trip_sheet_backend.models;

import java.time.LocalDate;

import com.example.trip_sheet_backend.common.models.BaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "vendor_employee_attendance",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_vendor_employee_attendance_day",
        columnNames = {"vendor_employee_id", "attendance_date"}
    ),
    indexes = @Index(name = "idx_vendor_employee_attendance_tenant_day", columnList = "tenant_id, attendance_date")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorEmployeeAttendance extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_employee_id", nullable = false)
    private VendorEmployee vendorEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    /** Unix epoch milliseconds. */
    @Column(name = "clock_in_at", nullable = false)
    private Long clockInAt;

    /** Unix epoch milliseconds. */
    @Column(name = "clock_out_at")
    private Long clockOutAt;

    /** Elapsed duty time, rounded down to whole minutes. */
    @Column(name = "worked_duration_minutes")
    private Long workedDurationMinutes;
}
