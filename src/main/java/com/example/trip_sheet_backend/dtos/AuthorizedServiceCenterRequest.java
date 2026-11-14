package com.example.trip_sheet_backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AuthorizedServiceCenterRequest {
  @NotBlank @Size(max = 180) private String name;
  @NotBlank @Size(max = 160) private String contactPersonName;
  @NotBlank @Size(max = 40) private String phoneNumber;
}
