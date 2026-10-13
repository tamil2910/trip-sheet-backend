package com.example.trip_sheet_backend.controllers;

import com.example.trip_sheet_backend.dtos.ChatBookingDraftRequest;
import com.example.trip_sheet_backend.dtos.ChatBookingDraftResponse;
import com.example.trip_sheet_backend.dtos.TripDtos.TripResponseDTO;
import com.example.trip_sheet_backend.mappers.TripResponseMapper;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.ChatBookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat/bookings")
public class ChatBookingController {
  private final ChatBookingService service;
  public ChatBookingController(ChatBookingService service) { this.service = service; }

  @PreAuthorize("hasAuthority('CAN_CREATE_TRIP')")
  @PostMapping("/draft")
  public ResponseEntity<ApiResponse<ChatBookingDraftResponse>> draft(@Valid @RequestBody ChatBookingDraftRequest body, HttpServletRequest request) {
    Tenant tenant=(Tenant)request.getAttribute("tenant");
    return ResponseEntity.ok(new ApiResponse<>(true,"Booking draft ready",service.createDraft(body.getMessage(),tenant)));
  }

  @PreAuthorize("hasAuthority('CAN_CREATE_TRIP')")
  @PostMapping("/{draftId}/confirm")
  public ResponseEntity<ApiResponse<TripResponseDTO>> confirm(@PathVariable UUID draftId, HttpServletRequest request) {
    Tenant tenant=(Tenant)request.getAttribute("tenant");
    UUID createdBy=(UUID)request.getAttribute("createdBy");
    Trip trip=service.confirm(draftId,tenant,createdBy);
    return ResponseEntity.ok(new ApiResponse<>(true,"Trip created successfully",TripResponseMapper.toDTO(trip,tenant.getId())));
  }
}
