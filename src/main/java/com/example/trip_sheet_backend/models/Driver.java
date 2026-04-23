package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "drivers",
    indexes = {
        @Index(name = "idx_driver_full_name", columnList = "fullName"),
        @Index(name = "idx_driver_license_number", columnList = "licenseNumber"),
    }
)
public class Driver extends BaseModel {

    @Column(name = "driverCode", unique = true)
    private String driverCode;

    @Column(name = "fullName")
    private String fullName;

    private String profilePicture;

    @Column(name = "licenseNumber")
    private String licenseNumber;
    private Long licenseExpiry;

    private String insuranceNumber;
    private Long insuranceExpiry;

    private String policeVerificationId;
    private String bloodGroup;

    private Double rating = 0.0;

    private Boolean active = true;
    private Boolean available = true;

    @Enumerated(EnumType.STRING)
    private DriverType driverType = DriverType.PERMANENT;

    // Link to the auth record
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_account_id", referencedColumnName = "id")
    private UserAccount account;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "role_id")
    // private Role role;

    public enum DriverType {
        PERMANENT, CONTRACT, TEMPORARY
    }
}