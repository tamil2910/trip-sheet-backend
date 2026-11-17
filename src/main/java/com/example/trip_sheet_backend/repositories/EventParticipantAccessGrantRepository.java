package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.EventParticipantAccessGrant;

public interface EventParticipantAccessGrantRepository extends BaseRepository<EventParticipantAccessGrant, UUID> {
  Optional<EventParticipantAccessGrant> findByTokenHashAndIsDeletedFalse(String tokenHash);
  java.util.List<EventParticipantAccessGrant> findAllByParticipantRequest_IdAndRevokedAtIsNullAndIsDeletedFalse(java.util.UUID requestId);
}