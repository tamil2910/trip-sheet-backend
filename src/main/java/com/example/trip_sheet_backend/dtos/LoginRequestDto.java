package com.example.trip_sheet_backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDto {
    private String identifier;  // can be email / phone / username
    private String password;
}
