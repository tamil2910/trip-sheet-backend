package com.example.trip_sheet_backend.services.TripService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.trip_sheet_backend.dtos.TripDtos.TripLabelAssignRequestDTO;
import com.example.trip_sheet_backend.models.Label;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.repositories.LabelRepository;
import com.example.trip_sheet_backend.repositories.TripRepository;

@ExtendWith(MockitoExtension.class)
class TripServiceImpLabelAssignmentTest {

  @Mock
  private TripRepository tripRepository;

  @Mock
  private LabelRepository labelRepository;

  @Test
  void assignLabelToTrip_setsProvidedLabelId() {
    UUID tenantId = UUID.randomUUID();
    UUID tripId = UUID.randomUUID();
    UUID labelId = UUID.randomUUID();

    Trip trip = new Trip();
    trip.setId(tripId);
    trip.setTenant(new Tenant());
    trip.getTenant().setId(tenantId);

    Label label = new Label();
    label.setId(labelId);
    label.setTenant(new Tenant());
    label.getTenant().setId(tenantId);

    when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
    when(labelRepository.findByIdAndTenant_IdAndIsDeletedFalse(labelId, tenantId)).thenReturn(Optional.of(label));
    when(tripRepository.save(trip)).thenReturn(trip);

    TripRealtimePublisher realtimePublisher = mock(TripRealtimePublisher.class);

    TripServiceImp service = new TripServiceImp(
        tripRepository,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        labelRepository,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        realtimePublisher
    );

    TripLabelAssignRequestDTO payload = new TripLabelAssignRequestDTO();
    payload.setLabelIds(List.of(labelId.toString()));

    Trip updatedTrip = service.assignLabelToTrip(new Tenant(), tenantId, tripId, payload, UUID.randomUUID());

    assertEquals(labelId, updatedTrip.getLabelId());
    assertEquals(List.of(labelId), updatedTrip.getLabels().stream().map(Label::getId).toList());
  }
}
