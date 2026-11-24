package com.example.trip_sheet_backend.dtos.SampleDtos;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SampleResponseDto {
    
    private UUID id;
    
    private String name;
    
    private Integer age;
    
    private String email;
    
    private String description;
    
    private Long createdAt;
    
    private Long updatedAt;
}