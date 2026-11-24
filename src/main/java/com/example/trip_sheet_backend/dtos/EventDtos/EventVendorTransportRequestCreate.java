package com.example.trip_sheet_backend.dtos.EventDtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EventVendorTransportRequestCreate {
  @NotBlank private String vendorId;
  @NotEmpty private List<@Valid Group> groups;

  @Getter @Setter
  public static class Group {
    @NotBlank private String transportType;
    @NotBlank private String vehicleTypeId;
    @NotBlank private String dutyTypeId;
    @jakarta.validation.constraints.NotNull private Long pickupTime;
    @NotEmpty private List<java.util.UUID> participantRequestIds;
  }
}
