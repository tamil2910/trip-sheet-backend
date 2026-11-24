package com.example.trip_sheet_backend.dtos.SampleDtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SampleCreateDto {
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotNull(message = "Age is required")
    private Integer age;
    
    @NotBlank(message = "Email is required")
    private String email;
    
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
    
    private String description;
}