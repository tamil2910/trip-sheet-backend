package com.example.trip_sheet_backend.dtos.EventDtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EventParticipantDecisionRequest {
  @NotNull
  private Decision decision;
  @Size(max = 1000)
  private String reason;

  public enum Decision { APPROVE, REJECT }
}