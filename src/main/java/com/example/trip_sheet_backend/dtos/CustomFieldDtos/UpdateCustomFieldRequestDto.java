package com.example.trip_sheet_backend.dtos.CustomFieldDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCustomFieldRequestDto {

  @NotBlank(message = "name is required")
  private String name;
}
