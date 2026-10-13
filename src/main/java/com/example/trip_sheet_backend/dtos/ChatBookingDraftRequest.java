package com.example.trip_sheet_backend.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ChatBookingDraftRequest {
  @NotBlank private String message;
}
