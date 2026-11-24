package com.example.trip_sheet_backend.dtos.SampleDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SampleUpdateDto {
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private Integer age;
    
    @NotBlank(message = "Email is required")
    private String email;
    
    private String description;
}