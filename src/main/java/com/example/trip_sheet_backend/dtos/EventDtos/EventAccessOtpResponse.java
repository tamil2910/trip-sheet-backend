package com.example.trip_sheet_backend.dtos.EventDtos;

import java.util.UUID;

public record EventAccessOtpResponse(UUID challengeId, Long expiresAt, String maskedEmail) { }