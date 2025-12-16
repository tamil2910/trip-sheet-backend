package com.example.trip_sheet_backend.dtos;

import java.util.List;

import com.example.trip_sheet_backend.models.Trip;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TripResponseDTO {

    private String id;

    private Trip.TripStatus tripStatus;

    private String bookingId;

    private String vendorId;
    private String vendorName;

    private String assignedByVendorId;
    private String assignedByVendorName;

    private String previousVendorId;
    private String previousVendorName;

    private String tenantId;
    private String tenantName;

    private String driverId;
    private String driverName;

    private String vehicleId;
    private String vehicleNumber;

    private String dutyTypeId;
    private String dutyTypeName;

    private String notes;

    private List<TripStopResponseDTO> stops;
}
