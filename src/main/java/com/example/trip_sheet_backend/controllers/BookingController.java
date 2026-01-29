package com.example.trip_sheet_backend.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.dtos.TripDtos.BookingCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.BookingResponseDTO;
import com.example.trip_sheet_backend.mappers.BookingResponseMapper;
// import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.models.Booking;
import com.example.trip_sheet_backend.models.Tenant;
// import com.example.trip_sheet_backend.models.Trip;
// import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.BookingService.BookingServiceImp;
import com.example.trip_sheet_backend.services.TripService.TripServiceImp;

import jakarta.annotation.security.PermitAll;
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
  public ResponseEntity<ApiResponse<?>> crateTrip(HttpServletRequest request, 
    @Valid @RequestBody BookingCreateRequestDTO createRequestDTO) {
    UUID createdBy = (UUID) request.getAttribute("createdBy");
    // UserAccount user = (UserAccount) request.getAttribute("user");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    // creating the trip
    Booking booking = this.bookingServiceImp.createBooking(createRequestDTO, tokenTenant, createdBy);

    BookingResponseDTO response = BookingResponseMapper.toDTO(booking);

    return ResponseEntity.ok().body(new ApiResponse<>(true, "Trip created successfully!", response));
  }
  // @PermitAll
  @GetMapping("/list")
  public ApiResponse<Map<String, Object>> getAll(
          Pageable pageable,
          HttpServletRequest request
  ) {
      UUID tenantId = (UUID) request.getAttribute("tenantId");

      Page<BookingResponseDTO> result = bookingServiceImp.getBookings(tenantId, pageable);

      Map<String, Object> response = new HashMap<>();

      response.put("data", result.getContent());

      response.put("currentPage", result.getNumber());
      response.put("pageSize", result.getSize());
      response.put("currentPageCount", result.getNumberOfElements());
      response.put("totalItems", result.getTotalElements());
      response.put("totalPages", result.getTotalPages());

      response.put("isFirst", result.isFirst());
      response.put("isLast", result.isLast());
      response.put("hasNext", result.hasNext());
      response.put("hasPrevious", result.hasPrevious());

      return new ApiResponse<>(true, "Bookings fetched successfully!", response);
  }




}
