package com.example.trip_sheet_backend.dtos.EventDtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EventRecipientRequest {
  @NotBlank
  private String name;
  @NotBlank
  @Email
  private String email;
  private String phone;
}
