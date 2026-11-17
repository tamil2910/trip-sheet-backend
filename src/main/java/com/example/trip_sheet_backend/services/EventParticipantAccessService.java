package com.example.trip_sheet_backend.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.trip_sheet_backend.dtos.EventDtos.EventAccessOtpResponse;
import com.example.trip_sheet_backend.dtos.EventDtos.EventAccessOtpVerificationRequest;
import com.example.trip_sheet_backend.dtos.EventDtos.EventAccessSessionResponse;
import com.example.trip_sheet_backend.dtos.EventDtos.EventParticipantPortalResponse;
import com.example.trip_sheet_backend.models.EventParticipantAccessGrant;
import com.example.trip_sheet_backend.models.EventParticipantAccessSession;
import com.example.trip_sheet_backend.models.EventParticipantOtpChallenge;
import com.example.trip_sheet_backend.models.EventParticipantRequest;
import com.example.trip_sheet_backend.models.TransportEvent;
import com.example.trip_sheet_backend.repositories.EventParticipantAccessGrantRepository;
import com.example.trip_sheet_backend.repositories.EventParticipantAccessSessionRepository;
import com.example.trip_sheet_backend.repositories.EventParticipantOtpChallengeRepository;
import com.example.trip_sheet_backend.repositories.EventParticipantRequestRepository;

@Service
public class EventParticipantAccessService {
  private static final Logger log = LoggerFactory.getLogger(EventParticipantAccessService.class);
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final long OTP_VALIDITY_MS = 10 * 60 * 1000L;
  private static final long OTP_RESEND_WAIT_MS = 60 * 1000L;
  private static final int MAX_OTP_ATTEMPTS = 5;

  private final EventParticipantAccessGrantRepository grantRepository;
  private final EventParticipantOtpChallengeRepository challengeRepository;
  private final EventParticipantAccessSessionRepository sessionRepository;
  private final EventParticipantRequestRepository requestRepository;
  private final EmailService emailService;
  private final PasswordEncoder passwordEncoder;
  private final String accessLinkBaseUrl;

  public EventParticipantAccessService(
      EventParticipantAccessGrantRepository grantRepository,
      EventParticipantOtpChallengeRepository challengeRepository,
      EventParticipantAccessSessionRepository sessionRepository,
      EventParticipantRequestRepository requestRepository,
      EmailService emailService,
      PasswordEncoder passwordEncoder,
      @Value("${app.event.access-link-base:http://localhost:4200/event-access}") String accessLinkBaseUrl) {
    this.grantRepository = grantRepository;
    this.challengeRepository = challengeRepository;
    this.sessionRepository = sessionRepository;
    this.requestRepository = requestRepository;
    this.emailService = emailService;
    this.passwordEncoder = passwordEncoder;
    this.accessLinkBaseUrl = accessLinkBaseUrl;
  }

  @Transactional
  public void issueForApproved(EventParticipantRequest participantRequest) {
    if (participantRequest == null || participantRequest.getStatus() != EventParticipantRequest.RequestStatus.APPROVED) return;
    if (participantRequest.getEmail() == null || participantRequest.getEmail().isBlank()) {
      throw new IllegalArgumentException("An email is required to issue event access");
    }
    TransportEvent event = participantRequest.getEvent();
    long now = Instant.now().toEpochMilli();
    Long expiresAt = event.getEndsAt();
    if (expiresAt == null || expiresAt <= now || event.getStatus() != TransportEvent.EventStatus.OPEN) return;

    grantRepository.findAllByParticipantRequest_IdAndRevokedAtIsNullAndIsDeletedFalse(participantRequest.getId())
        .forEach(grant -> grant.setRevokedAt(now));
    byte[] tokenBytes = new byte[32];
    RANDOM.nextBytes(tokenBytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

    EventParticipantAccessGrant grant = new EventParticipantAccessGrant();
    grant.setParticipantRequest(participantRequest);
    grant.setTokenHash(hashToken(token));
    grant.setExpiresAt(expiresAt);
    grant.setCreatedBy("EVENT_WORKFLOW");
    grantRepository.save(grant);

    String link = accessLinkBaseUrl.replaceAll("/+$", "") + "/" + token;
    String eventName = event.getName();
    String name = participantRequest.getName();
    String email = participantRequest.getEmail();
    runAfterCommit(() -> {
      try {
        emailService.sendEventParticipantAccessLink(email, name, eventName, link, expiresAt);
      } catch (RuntimeException ex) {
        log.error("Could not send event access link for request {}", participantRequest.getId(), ex);
      }
    });
  }

  @Transactional
  public EventAccessOtpResponse requestOtp(String rawLinkToken) {
    EventParticipantAccessGrant grant = findActiveGrant(rawLinkToken);
    long now = Instant.now().toEpochMilli();
    challengeRepository.findFirstByAccessGrant_IdAndIsDeletedFalseOrderByCreatedAtDesc(grant.getId())
        .ifPresent(previous -> {
          if (previous.getCreatedAt() != null && now - previous.getCreatedAt() < OTP_RESEND_WAIT_MS) {
            throw new IllegalArgumentException("Wait before requesting another OTP");
          }
        });

    String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
    EventParticipantOtpChallenge challenge = new EventParticipantOtpChallenge();
    challenge.setAccessGrant(grant);
    challenge.setOtpHash(passwordEncoder.encode(otp));
    challenge.setExpiresAt(Math.min(now + OTP_VALIDITY_MS, grant.getExpiresAt()));
    challenge.setAttempts(0);
    challenge.setCreatedBy("EVENT_PARTICIPANT");
    challengeRepository.save(challenge);

    String email = grant.getParticipantRequest().getEmail();
    String name = grant.getParticipantRequest().getName();
    String eventName = grant.getParticipantRequest().getEvent().getName();
    Long otpExpiry = challenge.getExpiresAt();
    runAfterCommit(() -> {
      try {
        emailService.sendEventParticipantOtp(email, name, eventName, otp, otpExpiry);
      } catch (RuntimeException ex) {
        log.error("Could not send event participant OTP for challenge {}", challenge.getId(), ex);
      }
    });
    return new EventAccessOtpResponse(challenge.getId(), challenge.getExpiresAt(), maskEmail(email));
  }

  @Transactional
  public EventAccessSessionResponse verifyOtp(EventAccessOtpVerificationRequest request) {
    EventParticipantOtpChallenge challenge = challengeRepository.findByIdAndIsDeletedFalse(request.getChallengeId())
        .orElseThrow(() -> new IllegalArgumentException("OTP challenge not found"));
    long now = Instant.now().toEpochMilli();
    if (challenge.getConsumedAt() != null || challenge.getExpiresAt() < now
        || challenge.getAttempts() >= MAX_OTP_ATTEMPTS) {
      throw new IllegalArgumentException("OTP has expired or can no longer be used");
    }
    if (!passwordEncoder.matches(request.getOtpCode(), challenge.getOtpHash())) {
      challenge.setAttempts(challenge.getAttempts() + 1);
      challengeRepository.save(challenge);
      return null;
    }

    EventParticipantAccessGrant grant = challenge.getAccessGrant();
    ensureGrantStillAllowed(grant, now);
    challenge.setConsumedAt(now);
    grant.setConsumedAt(now);
    challengeRepository.save(challenge);
    grantRepository.save(grant);

    byte[] tokenBytes = new byte[32];
    RANDOM.nextBytes(tokenBytes);
    String accessToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    EventParticipantAccessSession session = new EventParticipantAccessSession();
    session.setAccessGrant(grant);
    session.setTokenHash(hashToken(accessToken));
    session.setExpiresAt(grant.getExpiresAt());
    session.setCreatedBy("EVENT_PARTICIPANT");
    sessionRepository.save(session);
    return new EventAccessSessionResponse(accessToken, session.getExpiresAt());
  }

  @Transactional(readOnly = true)
  public EventParticipantPortalResponse getPortal(String rawSessionToken) {
    EventParticipantAccessSession session = findActiveSession(rawSessionToken);
    EventParticipantRequest request = session.getAccessGrant().getParticipantRequest();
    return EventParticipantPortalResponse.from(requestRepository.findByIdAndEvent_IdAndIsDeletedFalse(
        request.getId(), request.getEvent().getId()).orElseThrow(() -> new IllegalArgumentException("Event request not found")));
  }

  @Transactional
  public void logout(String rawSessionToken) {
    if (rawSessionToken == null || rawSessionToken.isBlank()) return;
    sessionRepository.findByTokenHashAndIsDeletedFalse(hashToken(rawSessionToken)).ifPresent(session -> {
      if (session.getRevokedAt() == null) {
        session.setRevokedAt(Instant.now().toEpochMilli());
        sessionRepository.save(session);
      }
    });
  }

  @Transactional
  public void withdraw(String rawSessionToken) {
    EventParticipantAccessSession session = findActiveSession(rawSessionToken);
    EventParticipantRequest request = session.getAccessGrant().getParticipantRequest();
    request.setStatus(EventParticipantRequest.RequestStatus.WITHDRAWN);
    request.setUpdatedBy("EVENT_PARTICIPANT");
    requestRepository.save(request);
    revokeForRequest(request);
  }

  private EventParticipantAccessSession findActiveSession(String rawSessionToken) {
    if (rawSessionToken == null || rawSessionToken.isBlank()) throw new IllegalArgumentException("Event access token is required");
    EventParticipantAccessSession session = sessionRepository.findByTokenHashAndIsDeletedFalse(hashToken(rawSessionToken))
        .orElseThrow(() -> new IllegalArgumentException("Event access session is invalid"));
    long now = Instant.now().toEpochMilli();
    if (session.getRevokedAt() != null || session.getExpiresAt() == null || session.getExpiresAt() <= now) {
      throw new IllegalArgumentException("Event access session has expired");
    }
    ensureGrantStillAllowed(session.getAccessGrant(), now);
    return session;
  }

  @Transactional
  public void revokeForRequest(EventParticipantRequest request) {
    if (request == null || request.getId() == null) return;
    long now = Instant.now().toEpochMilli();
    grantRepository.findAllByParticipantRequest_IdAndRevokedAtIsNullAndIsDeletedFalse(request.getId())
        .forEach(grant -> grant.setRevokedAt(now));
    sessionRepository.findAllByAccessGrant_ParticipantRequest_IdAndRevokedAtIsNullAndIsDeletedFalse(request.getId())
        .forEach(session -> session.setRevokedAt(now));
  }
  private EventParticipantAccessGrant findActiveGrant(String rawLinkToken) {
    if (rawLinkToken == null || rawLinkToken.isBlank()) throw new IllegalArgumentException("Access link token is required");
    EventParticipantAccessGrant grant = grantRepository.findByTokenHashAndIsDeletedFalse(hashToken(rawLinkToken))
        .orElseThrow(() -> new IllegalArgumentException("Access link is invalid or expired"));
    ensureGrantStillAllowed(grant, Instant.now().toEpochMilli());
    if (grant.getConsumedAt() != null) throw new IllegalArgumentException("Access link was already used");
    return grant;
  }

  private void ensureGrantStillAllowed(EventParticipantAccessGrant grant, long now) {
    EventParticipantRequest request = grant.getParticipantRequest();
    TransportEvent event = request.getEvent();
    if (grant.getRevokedAt() != null || grant.getExpiresAt() == null || grant.getExpiresAt() <= now
        || request.getStatus() != EventParticipantRequest.RequestStatus.APPROVED
        || event.getStatus() != TransportEvent.EventStatus.OPEN
        || event.getEndsAt() == null || event.getEndsAt() <= now) {
      throw new IllegalArgumentException("Event access is no longer active");
    }
  }

  private String hashToken(String token) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }

  private String maskEmail(String email) {
    int at = email.indexOf('@');
    if (at <= 1) return "***" + email.substring(Math.max(at, 0));
    return email.substring(0, 1) + "***" + email.substring(at);
  }

  private void runAfterCommit(Runnable action) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override public void afterCommit() { action.run(); }
      });
    } else {
      action.run();
    }
  }
}