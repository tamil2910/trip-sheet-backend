package com.example.trip_sheet_backend.models;

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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vendor_employee_addresses", indexes = @Index(
        name = "idx_vendor_employee_address_employee", columnList = "vendor_employee_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorEmployeeAddress extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_employee_id", nullable = false)
    private VendorEmployee vendorEmployee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AddressCategory category;

    @Column(name = "address_line_1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line_2")
    private String addressLine2;

    private String landmark;
    private String city;
    private String district;
    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    private String country;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    public enum AddressCategory {
        HOME, OFFICE, OTHER
    }
}
