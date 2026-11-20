package com.example.trip_sheet_backend.dtos.EventDtos;

import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EventParticipantTransportTypesUpdateRequest {
  @NotNull
  @Size(max = 3)
  private List<@NotBlank @Size(max = 32) String> transportTypes;
}
