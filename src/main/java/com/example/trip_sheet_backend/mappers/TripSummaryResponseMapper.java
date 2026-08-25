package com.example.trip_sheet_backend.mappers;

import com.example.trip_sheet_backend.dtos.TripDtos.TripSummaryResponseDTO;
import com.example.trip_sheet_backend.models.TripSummary;

public final class TripSummaryResponseMapper {
  private TripSummaryResponseMapper() {
  }

  public static TripSummaryResponseDTO toDTO(TripSummary summary) {
    TripSummaryResponseDTO dto = new TripSummaryResponseDTO();
    if (summary.getId() != null) dto.setId(summary.getId().toString());
    if (summary.getTripId() != null && summary.getTripId().getId() != null) dto.setTripId(summary.getTripId().getId().toString());
    if (summary.getTripId() != null) dto.setTrip(TripResponseMapper.toDTO(summary.getTripId()));
    dto.setGarageStartTime(summary.getGarageStartTime());
    dto.setGarageEndTime(summary.getGarageEndTime());
    dto.setTripArrivedTime(summary.getTripArrivedTime());
    dto.setTripStartTime(summary.getTripStartTime());
    dto.setTripStartKmOdo(summary.getTripStartKmOdo());
    dto.setTripStartKmOdoImage(summary.getTripStartKmOdoImage());
    dto.setTripEndTime(summary.getTripEndTime());
    dto.setTripEndKmOdo(summary.getTripEndKmOdo());
    dto.setTripEndKmOdoImage(summary.getTripEndKmOdoImage());
    dto.setTripDuration(summary.getTripDuration());
    dto.setTripDistance(summary.getTripDistance());
    dto.setTripExtraKmOdo(summary.getTripExtraKmOdo());
    dto.setTripExtraKm(summary.getTripExtraKm());
    dto.setTripExtraHr(summary.getTripExtraHr());
    dto.setTripStartGPSKM(summary.getTripStartGPSKM());
    dto.setTripEndGPSKM(summary.getTripEndGPSKM());
    dto.setTripGPSDuration(summary.getTripGPSDuration());
    dto.setTripGPSDistance(summary.getTripGPSDistance());
    dto.setDispatchLat(summary.getDispatchLat());
    dto.setDispatchLng(summary.getDispatchLng());
    dto.setArrivedLat(summary.getArrivedLat());
    dto.setArrivedLng(summary.getArrivedLng());
    dto.setTripStartLat(summary.getTripStartLat());
    dto.setTripStartLng(summary.getTripStartLng());
    dto.setTripEndLat(summary.getTripEndLat());
    dto.setTripEndLng(summary.getTripEndLng());
    dto.setGarageEndLat(summary.getGarageEndLat());
    dto.setGarageEndLng(summary.getGarageEndLng());
    return dto;
  }
}
