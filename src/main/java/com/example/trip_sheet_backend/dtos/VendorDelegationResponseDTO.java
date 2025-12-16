package com.example.trip_sheet_backend.dtos;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VendorDelegationResponseDTO {

    private String id; // history id

    private String tripId;

    private String fromVendorId;
    private String fromVendorName;

    private String toVendorId;
    private String toVendorName;

    private LocalDateTime delegatedAt;
}
