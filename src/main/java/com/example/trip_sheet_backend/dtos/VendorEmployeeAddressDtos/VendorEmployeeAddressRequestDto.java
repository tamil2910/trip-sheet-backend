package com.example.trip_sheet_backend.dtos.VendorEmployeeAddressDtos;

import com.example.trip_sheet_backend.models.VendorEmployeeAddress.AddressCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VendorEmployeeAddressRequestDto {
    @NotNull
    private AddressCategory category;

    @NotBlank
    private String addressLine1;

    private String addressLine2;
    private String landmark;
    private String city;
    private String district;
    private String state;
    private String postalCode;
    private String country;
}
