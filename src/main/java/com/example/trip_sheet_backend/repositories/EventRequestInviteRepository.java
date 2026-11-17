package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.EventRequestInvite;

public interface EventRequestInviteRepository extends BaseRepository<EventRequestInvite, UUID> {
  List<EventRequestInvite> findAllByEvent_IdAndRevokedAtIsNullAndIsDeletedFalse(UUID eventId);
  Optional<EventRequestInvite> findByTokenHashAndIsDeletedFalse(String tokenHash);
}
