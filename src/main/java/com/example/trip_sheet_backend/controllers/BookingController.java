package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.models.Booking;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.BookingService.BookingServiceImp;

import jakarta.servlet.http.HttpServletRequest;



@RestController
@RequestMapping("/bookings")
public class BookingController extends BaseController<Booking, UUID>{
  private final BookingServiceImp service;
  public BookingController(BookingServiceImp service) {
    super(service);
    this.service = service;
  }

  // @PreAuthorize("hasAuthority('CAN_CREATE_TRIP')")
  @PostMapping("/create_trip")
  public ResponseEntity<ApiResponse<?>> crateTrip(HttpServletRequest request) {
    UUID createdBy = (UUID) request.getAttribute("createdBy");
    UUID userId = (UUID) request.getAttribute("userId");
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    return ResponseEntity.ok(new ApiResponse<>(true, "Trip Created Successfully!", null));
  }


}
