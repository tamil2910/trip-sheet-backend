package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shifts")
public class Shift extends BaseModel implements TenantScoped {

    @NotBlank(message = "Shift name is required")
    @Column(name = "shift_name", nullable = false)
    private String name;

    @NotNull(message = "Shift start time is required")
    @Column(name = "start_time", nullable = false)
    private String startTime;

    @NotNull(message = "Shift end time is required")
    @Column(name = "end_time", nullable = false)
    private String endTime;

    @NotNull(message = "Tenant is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
}
