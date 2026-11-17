package com.example.trip_sheet_backend.repositories;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.EventParticipantRequest;

public interface EventParticipantRequestRepository extends BaseRepository<EventParticipantRequest, UUID> {
  List<EventParticipantRequest> findAllByEvent_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID eventId);
  List<EventParticipantRequest> findAllByEvent_IdAndStatusAndIsDeletedFalseOrderByCreatedAtAsc(
      UUID eventId, EventParticipantRequest.RequestStatus status);
  Optional<EventParticipantRequest> findByIdAndEvent_IdAndIsDeletedFalse(UUID id, UUID eventId);
}
