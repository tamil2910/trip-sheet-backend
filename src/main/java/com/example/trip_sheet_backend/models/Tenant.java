package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
// import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tenant extends BaseModel {

    @NotBlank(message = "Tenant name is required")
    @Size(min = 2, message = "Tenant name must contain at least 2 characters")
    @Column(name = "tenant_name", nullable = false)
    private String tenantName;

    @Email(message = "Invalid email format")
    @Column(unique = true, nullable = false)
    private String contactEmail;

    @Column(unique = true)
    private String tenantUniqueCode;

    @JsonIgnore
    // @Valid
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = true)
    private Admin admin;

    @Enumerated(EnumType.STRING)
    private TenantType tenantType;   

    private String gstNumber;

    private String address;

    public enum TenantType {
        VENDOR, ORGANISATION
    }

    private Boolean isActive = false;

    private Boolean verifiedGst = false;
}
