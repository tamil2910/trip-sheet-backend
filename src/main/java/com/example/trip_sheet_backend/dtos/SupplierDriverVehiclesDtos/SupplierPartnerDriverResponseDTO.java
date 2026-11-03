package com.example.trip_sheet_backend.dtos.SupplierDriverVehiclesDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.SupplierDriverVehicles;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupplierPartnerDriverResponseDTO {
    private UUID id;
    private UUID primaryVendorId;
    private UUID partnerVendorId;
    private String driverName;
    private String phone;
    private String email;
    private UUID driverId;

    public static SupplierPartnerDriverResponseDTO fromEntity(SupplierDriverVehicles entry) {
        return new SupplierPartnerDriverResponseDTO(entry.getId(), entry.getPrimaryVendor().getId(),
                entry.getPartnerVendor().getId(), entry.getDriverName(), entry.getPhone(), entry.getEmail(),
                entry.getDriver() == null ? null : entry.getDriver().getId());
    }
}
