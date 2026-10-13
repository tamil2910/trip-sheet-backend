package com.example.trip_sheet_backend.dtos;

import jakarta.validation.constraints.NotBlank;

public record MediaUploadRequest(String category, @NotBlank String base64) {}
