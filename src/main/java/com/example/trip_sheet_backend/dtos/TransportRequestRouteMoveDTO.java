package com.example.trip_sheet_backend.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TransportRequestRouteMoveDTO {
    @NotBlank(message = "routeId is required")
    private String routeId;
}
