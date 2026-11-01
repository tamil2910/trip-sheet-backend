package com.example.trip_sheet_backend.dtos.VendorEmployeeAddressDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.VendorEmployeeAddress;
import com.example.trip_sheet_backend.models.VendorEmployeeAddress.AddressCategory;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorEmployeeAddressResponseDto {
    private UUID id;
    private UUID vendorEmployeeId;
    private AddressCategory category;
    private String addressLine1;
    private String addressLine2;
    private String landmark;
    private String city;
    private String district;
    private String state;
    private String postalCode;
    private String country;
    private Double latitude;
    private Double longitude;

    public VendorEmployeeAddressResponseDto(VendorEmployeeAddress address) {
        this.id = address.getId();
        this.vendorEmployeeId = address.getVendorEmployee().getId();
        this.category = address.getCategory();
        this.addressLine1 = address.getAddressLine1();
        this.addressLine2 = address.getAddressLine2();
        this.landmark = address.getLandmark();
        this.city = address.getCity();
        this.district = address.getDistrict();
        this.state = address.getState();
        this.postalCode = address.getPostalCode();
        this.country = address.getCountry();
        this.latitude = address.getLatitude();
        this.longitude = address.getLongitude();
    }
}
