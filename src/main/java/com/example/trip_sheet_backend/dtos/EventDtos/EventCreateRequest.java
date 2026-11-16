package com.example.trip_sheet_backend.dtos.EventDtos;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.TransportEvent.RequestAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EventCreateRequest {
  @NotBlank
  private String name;
  @NotBlank
  private String venue;
  private String city;
  private String description;
  private Double venueLatitude;
  private Double venueLongitude;
  @NotNull
  private Long startsAt;
  @NotNull
  private Long endsAt;
  @NotNull
  private Long linkValidUntil;
  @NotNull
  private RequestAudience requestAudience;
  private List<UUID> vehicleTypeIds;
}


