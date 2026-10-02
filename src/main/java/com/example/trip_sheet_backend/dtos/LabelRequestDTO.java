package com.example.trip_sheet_backend.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LabelRequestDTO {
  @NotBlank(message = "Label name is required")
  private String name;

  private String color;
}