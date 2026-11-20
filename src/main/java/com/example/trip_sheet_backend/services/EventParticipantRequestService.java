package com.example.trip_sheet_backend.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.EventDtos.EventParticipantAdminResponse;
import com.example.trip_sheet_backend.dtos.EventDtos.EventParticipantDecisionRequest;
import com.example.trip_sheet_backend.dtos.EventDtos.EventRequestLinkResponse;
import com.example.trip_sheet_backend.dtos.EventDtos.EventRequestSubmission;
import com.example.trip_sheet_backend.dtos.EventDtos.EventRequestSubmissionResponse;
import com.example.trip_sheet_backend.dtos.EventDtos.EventResponse;
import com.example.trip_sheet_backend.models.EventParticipantRequest;
import com.example.trip_sheet_backend.models.EventRequestInvite;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.TransportEvent;
import com.example.trip_sheet_backend.repositories.EventParticipantRequestRepository;
import com.example.trip_sheet_backend.repositories.EventRequestInviteRepository;
import com.example.trip_sheet_backend.repositories.PeopleTenantRepository;
import com.example.trip_sheet_backend.repositories.TransportEventRepository;

@Service
public class EventParticipantRequestService {
  private final TransportEventRepository eventRepository;
  private final EventRequestInviteRepository inviteRepository;
  private final EventParticipantRequestRepository requestRepository;
  private final PeopleTenantRepository peopleTenantRepository;
  private final EventParticipantAccessService accessService;

  public EventParticipantRequestService(
      TransportEventRepository eventRepository,
      EventRequestInviteRepository inviteRepository,
      EventParticipantRequestRepository requestRepository,
      PeopleTenantRepository peopleTenantRepository,
      EventParticipantAccessService accessService) {
    this.eventRepository = eventRepository;
    this.inviteRepository = inviteRepository;
    this.requestRepository = requestRepository;
    this.peopleTenantRepository = peopleTenantRepository;
    this.accessService = accessService;
  }

  @Transactional(readOnly = true)
  public EventRequestLinkResponse inspectLink(String rawToken) {
    LinkContext context = resolveLink(rawToken);
    TransportEvent event = context.event();
    String invitedName = context.invite() == null ? null : context.invite().getName();
    return new EventRequestLinkResponse(event.getId(), event.getName(), event.getVenue(), event.getCity(),
        event.getDescription(), event.isBringFamily(), event.getAllowedExtraPassengers(), event.getVenueLatitude(), event.getVenueLongitude(), event.getStartsAt(),
        event.getEndsAt(), event.getLinkValidUntil(), invitedName, List.copyOf(event.getTransportTypes()),
        event.getVehicleTypeMappings().stream()
            .map(mapping -> EventResponse.VehicleTypeOption.from(mapping.getVehicleType(), mapping.getAllowedPassenger())).toList(),
        event.getDutyTypes().stream().map(EventResponse.DutyTypeOption::from).toList());
  }

  @Transactional
  public EventRequestSubmissionResponse submit(String rawToken, EventRequestSubmission body) {
    LinkContext context = resolveLink(rawToken);
    TransportEvent event = context.event();
    EventRequestInvite invite = context.invite();
    String email = normalizeEmail(body.getEmail());
    String phone = trimToNull(body.getPhone());
    if (email == null && phone == null) throw new IllegalArgumentException("Provide a phone number or email address");
    if (invite != null && !invite.getEmail().equalsIgnoreCase(email == null ? "" : email)) {
      throw new IllegalArgumentException("Use the email address this invitation was sent to");
    }

    MatchResult match = findPeopleTenant(event, email, phone);
    EventParticipantRequest participantRequest = new EventParticipantRequest();
    participantRequest.setEvent(event);
    participantRequest.setInvite(invite);
    participantRequest.setPeopleTenant(match.person());
    participantRequest.setName(body.getName().trim());
    participantRequest.setEmail(email);
    participantRequest.setPhone(phone);
    participantRequest.setGender(trimToNull(body.getGender()));
    if (!event.isBringFamily() && body.getExtraPassengers() != null && !body.getExtraPassengers().isEmpty()) {
      throw new IllegalArgumentException("Family members are not allowed for this event");
    }
    if (body.getExtraPassengers() != null) {
      if (body.getExtraPassengers().size() > event.getAllowedExtraPassengers()) {
        throw new IllegalArgumentException("Extra passengers exceed the event's per-participant limit of " + event.getAllowedExtraPassengers());
      }
      body.getExtraPassengers().forEach(passenger -> {
        com.example.trip_sheet_backend.models.EventParticipantFamilyMember extra = new com.example.trip_sheet_backend.models.EventParticipantFamilyMember();
        extra.setParticipantRequest(participantRequest);
        extra.setName(passenger.getName().trim());
        extra.setGender(trimToNull(passenger.getGender()));
        extra.setRelationshipType(passenger.getRelationshipType().trim());
        participantRequest.getExtraPassengers().add(extra);
      });
    }
    participantRequest.setPickupAddress(trimToNull(body.getPickupAddress()));
    participantRequest.setPickupLatitude(body.getPickupLatitude());
    participantRequest.setPickupLongitude(body.getPickupLongitude());
    participantRequest.setNotes(trimToNull(body.getNotes()));
    participantRequest.setMatchMethod(match.method());
    participantRequest.setStatus(match.person() != null
        ? EventParticipantRequest.RequestStatus.APPROVED
        : EventParticipantRequest.RequestStatus.PENDING_APPROVAL);
    participantRequest.setCreatedBy("EVENT_PARTICIPANT");
    participantRequest.setUpdatedBy("EVENT_PARTICIPANT");
    EventParticipantRequest saved = requestRepository.save(participantRequest);

    if (invite != null) {
      invite.setConsumedAt(Instant.now().toEpochMilli());
      inviteRepository.save(invite);
    }
    if (saved.getStatus() == EventParticipantRequest.RequestStatus.APPROVED) {
      accessService.issueForApproved(saved);
    }

    boolean approved = saved.getStatus() == EventParticipantRequest.RequestStatus.APPROVED;
    return new EventRequestSubmissionResponse(saved.getId(), event.getId(), event.getName(), saved.getStatus(),
        match.person() != null, saved.getGender(), saved.getExtraPassengers().stream().map(extra -> new EventRequestSubmissionResponse.ExtraPassenger(extra.getName(), extra.getGender(), extra.getRelationshipType())).toList(), approved
            ? "Request submitted and approved. A temporary access link will be emailed."
            : "Request submitted and is waiting for admin approval");
  }

  @Transactional(readOnly = true)
  public List<EventParticipantAdminResponse> listForReview(UUID eventId, EventParticipantRequest.RequestStatus status, Tenant tenant) {
    requireTenant(tenant);
    eventRepository.findByIdAndTenant_IdAndIsDeletedFalse(eventId, tenant.getId())
        .orElseThrow(() -> new IllegalArgumentException("Event not found"));
    return requestRepository.findAllByEvent_IdAndIsDeletedFalseOrderByCreatedAtDesc(eventId).stream()
        .filter(request -> status == null || request.getStatus() == status)
        .map(EventParticipantAdminResponse::from)
        .toList();
  }

  @Transactional
  public EventParticipantAdminResponse decide(UUID eventId, UUID requestId,
      EventParticipantDecisionRequest.Decision decision, String reason, Tenant tenant, UUID actorId) {
    requireTenant(tenant);
    TransportEvent event = eventRepository.findByIdAndTenant_IdAndIsDeletedFalse(eventId, tenant.getId())
        .orElseThrow(() -> new IllegalArgumentException("Event not found"));
    EventParticipantRequest request = requestRepository.findByIdAndEvent_IdAndIsDeletedFalse(requestId, eventId)
        .orElseThrow(() -> new IllegalArgumentException("Participant request not found"));
    if (decision == EventParticipantDecisionRequest.Decision.APPROVE && event.getEndsAt() != null
        && event.getEndsAt() <= Instant.now().toEpochMilli()) {
      throw new IllegalArgumentException("An event cannot be approved after it has ended");
    }
    if (request.getStatus() != EventParticipantRequest.RequestStatus.PENDING_APPROVAL) {
      throw new IllegalArgumentException("Only pending participant requests can be reviewed");
    }

    request.setStatus(decision == EventParticipantDecisionRequest.Decision.APPROVE
        ? EventParticipantRequest.RequestStatus.APPROVED
        : EventParticipantRequest.RequestStatus.REJECTED);
    request.setReviewedAt(Instant.now().toEpochMilli());
    request.setReviewedBy(actorId == null ? null : actorId.toString());
    request.setDecisionReason(reason == null || reason.isBlank() ? null : reason.trim());
    request.setUpdatedBy(actorId == null ? null : actorId.toString());
    EventParticipantRequest saved = requestRepository.save(request);
    if (saved.getStatus() == EventParticipantRequest.RequestStatus.APPROVED) accessService.issueForApproved(saved);
    else accessService.revokeForRequest(saved);
    return EventParticipantAdminResponse.from(saved);
  }

  private LinkContext resolveLink(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) throw new IllegalArgumentException("Request link token is required");
    String tokenHash = hashToken(rawToken);
    long now = Instant.now().toEpochMilli();

    TransportEvent publicEvent = eventRepository.findByPublicRequestTokenHashAndIsDeletedFalse(tokenHash).orElse(null);
    if (publicEvent != null) {
      validateEventOpen(publicEvent, now);
      if (publicEvent.getRequestAudience() != TransportEvent.RequestAudience.ANYONE_WITH_LINK) {
        throw new IllegalArgumentException("This event link is no longer valid");
      }
      return new LinkContext(publicEvent, null);
    }

    EventRequestInvite invite = inviteRepository.findByTokenHashAndIsDeletedFalse(tokenHash)
        .orElseThrow(() -> new IllegalArgumentException("Request link is invalid or expired"));
    TransportEvent event = invite.getEvent();
    validateEventOpen(event, now);
    if (event.getRequestAudience() != TransportEvent.RequestAudience.SPECIFIC_PEOPLE
        || invite.getRevokedAt() != null
        || invite.getConsumedAt() != null
        || invite.getExpiresAt() == null
        || invite.getExpiresAt() < now) {
      throw new IllegalArgumentException("Request link is invalid, expired, or already used");
    }
    return new LinkContext(event, invite);
  }

  private void validateEventOpen(TransportEvent event, long now) {
    if (Boolean.TRUE.equals(event.getIsDeleted())
        || event.getStatus() != TransportEvent.EventStatus.OPEN
        || event.getLinkValidUntil() == null
        || event.getLinkValidUntil() < now
        || event.getEndsAt() == null
        || event.getEndsAt() <= now) {
      throw new IllegalArgumentException("Event request link has expired or is closed");
    }
  }

  private MatchResult findPeopleTenant(TransportEvent event, String email, String phone) {
    Tenant organisation = event.getTenant();
    if (organisation == null || organisation.getId() == null) return new MatchResult(null, EventParticipantRequest.MatchMethod.NONE);

    List<PeopleTenant> emailMatches = email == null ? List.of()
        : peopleTenantRepository.findAllByEmailIgnoreCaseAndOrganisation_Id(email, organisation.getId());
    List<PeopleTenant> phoneMatches = phone == null ? List.of()
        : peopleTenantRepository.findAllByPhoneAndOrganisation_Id(phone, organisation.getId());
    Map<UUID, PeopleTenant> candidates = new LinkedHashMap<>();
    emailMatches.forEach(person -> candidates.put(person.getId(), person));
    phoneMatches.forEach(person -> candidates.put(person.getId(), person));
    if (candidates.isEmpty()) return new MatchResult(null, EventParticipantRequest.MatchMethod.NONE);
    if (candidates.size() > 1) return new MatchResult(null, EventParticipantRequest.MatchMethod.AMBIGUOUS);

    PeopleTenant matched = candidates.values().iterator().next();
    boolean emailMatched = emailMatches.stream().anyMatch(person -> person.getId().equals(matched.getId()));
    boolean phoneMatched = phoneMatches.stream().anyMatch(person -> person.getId().equals(matched.getId()));
    if (!emailMatched && phoneMatched && matched.getEmail() != null && !matched.getEmail().isBlank()
        && !matched.getEmail().equalsIgnoreCase(email)) {
      return new MatchResult(null, EventParticipantRequest.MatchMethod.AMBIGUOUS);
    }
    EventParticipantRequest.MatchMethod method = emailMatched && phoneMatched
        ? EventParticipantRequest.MatchMethod.BOTH
        : emailMatched ? EventParticipantRequest.MatchMethod.EMAIL : EventParticipantRequest.MatchMethod.PHONE;
    return new MatchResult(matched, method);
  }

  private void requireTenant(Tenant tenant) {
    if (tenant == null || tenant.getId() == null) throw new IllegalArgumentException("Tenant not found in token");
  }

  private String normalizeEmail(String value) {
    return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String hashToken(String token) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }

  private record LinkContext(TransportEvent event, EventRequestInvite invite) { }
  private record MatchResult(PeopleTenant person, EventParticipantRequest.MatchMethod method) { }
}
