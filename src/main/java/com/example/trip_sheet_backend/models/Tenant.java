package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.*;
import jakarta.validation.Valid;
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
    private String contact_email;

    @Valid
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @Enumerated(EnumType.STRING)
    private TenantType tenant_type;   

    private String gst_number;

    private String address;

    private enum TenantType {
        VENDOR, ORGANISATION
    }

    private Boolean verified_gst = false;
}
