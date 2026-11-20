package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.EventDtos.EventAccessOtpResponse;
import com.example.trip_sheet_backend.dtos.EventDtos.EventAccessOtpVerificationRequest;
import com.example.trip_sheet_backend.dtos.EventDtos.EventAccessSessionResponse;
import com.example.trip_sheet_backend.dtos.EventDtos.EventParticipantPortalResponse;
import com.example.trip_sheet_backend.dtos.EventDtos.EventParticipantTransportTypesUpdateRequest;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.EventParticipantAccessService;

import jakarta.validation.Valid;

@RestController
public class EventParticipantAccessController {
  private final EventParticipantAccessService accessService;

  public EventParticipantAccessController(EventParticipantAccessService accessService) {
    this.accessService = accessService;
  }

  @PostMapping("/event-access/{linkToken}/otp")
  public ApiResponse<EventAccessOtpResponse> requestOtp(@PathVariable String linkToken) {
    return new ApiResponse<>(true, "Verification code sent", accessService.requestOtp(linkToken));
  }

  @PostMapping("/event-access/verify-otp")
  public ApiResponse<EventAccessSessionResponse> verifyOtp(@Valid @RequestBody EventAccessOtpVerificationRequest body) {
    EventAccessSessionResponse session = accessService.verifyOtp(body);
    return session == null
        ? new ApiResponse<>(false, "Invalid verification code", null)
        : new ApiResponse<>(true, "Event participant signed in", session);
  }

  @GetMapping("/event-portal/me")
  public ApiResponse<EventParticipantPortalResponse> me(
      @RequestHeader(value = "X-Event-Access-Token", required = false) String accessToken) {
    return new ApiResponse<>(true, "Event participant details fetched", accessService.getPortal(accessToken));
  }

  @PutMapping("/event-portal/me")
  public ApiResponse<EventParticipantPortalResponse> updateTransportTypes(
      @RequestHeader(value = "X-Event-Access-Token", required = false) String accessToken,
      @Valid @RequestBody EventParticipantTransportTypesUpdateRequest body) {
    return new ApiResponse<>(true, "Event participant transport types updated",
        accessService.updateTransportTypes(accessToken, body));
  }

  @PostMapping("/event-portal/logout")
  public ApiResponse<Void> logout(
      @RequestHeader(value = "X-Event-Access-Token", required = false) String accessToken) {
    accessService.logout(accessToken);
    return new ApiResponse<>(true, "Event participant signed out", null);
  }

  @PostMapping("/event-portal/withdraw")
  public ApiResponse<Void> withdraw(
      @RequestHeader(value = "X-Event-Access-Token", required = false) String accessToken) {
    accessService.withdraw(accessToken);
    return new ApiResponse<>(true, "Event request withdrawn", null);
  }
}
