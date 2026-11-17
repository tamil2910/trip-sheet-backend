package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.EventParticipantOtpChallenge;

public interface EventParticipantOtpChallengeRepository extends BaseRepository<EventParticipantOtpChallenge, UUID> {
  Optional<EventParticipantOtpChallenge> findByIdAndIsDeletedFalse(UUID id);
  Optional<EventParticipantOtpChallenge> findFirstByAccessGrant_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID grantId);
}