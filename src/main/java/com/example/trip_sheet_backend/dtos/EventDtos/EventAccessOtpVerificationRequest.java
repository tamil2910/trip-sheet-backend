package com.example.trip_sheet_backend.dtos.EventDtos;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EventAccessOtpVerificationRequest {
  @NotNull
  private UUID challengeId;
  @NotBlank
  private String otpCode;
}