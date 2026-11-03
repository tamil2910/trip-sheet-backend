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
public class SupplierPartnerVehicleResponseDTO {
    private UUID id;
    private UUID primaryVendorId;
    private UUID partnerVendorId;
    private String vehicleNumber;
    private UUID vehicleTypeId;

    public static SupplierPartnerVehicleResponseDTO fromEntity(SupplierDriverVehicles entry) {
        return new SupplierPartnerVehicleResponseDTO(entry.getId(), entry.getPrimaryVendor().getId(),
                entry.getPartnerVendor().getId(), entry.getVehicleNumber(),
                entry.getVehicleType() == null ? null : entry.getVehicleType().getId());
    }
}
