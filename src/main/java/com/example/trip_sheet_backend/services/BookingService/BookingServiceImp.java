package com.example.trip_sheet_backend.services.BookingService;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.models.Booking;
import com.example.trip_sheet_backend.repositories.BookingRepository;

@Service
public class BookingServiceImp extends BaseServiceImp<Booking, UUID> implements BookingService {
  private final BookingRepository repository;
  public BookingServiceImp(BookingRepository repository) {
    super(repository);
    this.repository = repository;
  }


}
