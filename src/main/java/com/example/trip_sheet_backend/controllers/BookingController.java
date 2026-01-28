package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.dtos.TripDtos.BookingCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.models.Booking;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
// import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.BookingService.BookingServiceImp;
import com.example.trip_sheet_backend.services.TripService.TripServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;



@RestController
@RequestMapping("/bookings")
public class BookingController extends BaseController<Booking, UUID>{
  private final BookingServiceImp bookingServiceImp;
  private final TripServiceImp tripServiceImp;
  public BookingController(BookingServiceImp bookingServiceImp, TripServiceImp tripServiceImp) {
    super(bookingServiceImp);
    this.tripServiceImp = tripServiceImp;
    this.bookingServiceImp = bookingServiceImp;
  }

  @PreAuthorize("hasAuthority('CAN_CREATE_TRIP')")
  @PostMapping("/create")
  public ResponseEntity<ApiResponse<Booking>> crateTrip(HttpServletRequest request, 
    @Valid @RequestBody BookingCreateRequestDTO createRequestDTO) {
    UUID createdBy = (UUID) request.getAttribute("createdBy");
    // UserAccount user = (UserAccount) request.getAttribute("user");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    // creating the trip
    Booking booking = this.bookingServiceImp.createBooking(createRequestDTO, tokenTenant, createdBy);
    return ResponseEntity.ok(new ApiResponse<>(true, "Trip Created Successfully!", null));
  }


}
