package com.example.trip_sheet_backend.dtos;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class ChatBookingDraftResponse {
  private UUID draftId;
  private String status;
  private Booking booking;
  private List<String> questions;
  @Getter @AllArgsConstructor
  public static class Booking {
    private String organisationName;
    private String passengerName;
    private String dutyTypeName;
    private String vehicleTypeName;
    private String city;
    private String pickupDate;
    private String pickupTime;
  }
}
