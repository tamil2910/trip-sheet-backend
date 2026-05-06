package com.example.trip_sheet_backend.dtos.CustomFieldDtos;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateCustomFieldRequestDto {

  @NotBlank(message = "name is required")
  private String name;

  private UUID organisationId;
}
