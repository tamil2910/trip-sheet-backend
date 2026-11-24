package com.example.trip_sheet_backend.services;

import com.example.trip_sheet_backend.dtos.EventDtos.*;
import com.example.trip_sheet_backend.dtos.TripDtos.TripAllotRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripStopRequestDTO;
import com.example.trip_sheet_backend.models.*;
import com.example.trip_sheet_backend.repositories.*;
import com.example.trip_sheet_backend.services.TripService.TripServiceImp;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventVendorTransportService {
  private final TransportEventRepository events;
  private final EventParticipantRequestRepository participants;
  private final EventVendorTransportRequestRepository requests;
  private final TenantRepository tenants;
  private final VendorOrganisationRepository vendorOrganisations;
  private final VendorPartnerRepository vendorPartners;
  private final TripServiceImp trips;

  public EventVendorTransportService(TransportEventRepository events, EventParticipantRequestRepository participants,
      EventVendorTransportRequestRepository requests, TenantRepository tenants,
      VendorOrganisationRepository vendorOrganisations, VendorPartnerRepository vendorPartners,
      TripServiceImp trips) {
    this.events = events; this.participants = participants; this.requests = requests; this.tenants = tenants;
    this.vendorOrganisations = vendorOrganisations; this.vendorPartners = vendorPartners;
    this.trips = trips;
  }

  @Transactional
  public List<EventVendorTransportRequestResponse> send(UUID eventId, EventVendorTransportRequestCreate body,
      Tenant organisation, UUID actorId) {
    TransportEvent event = events.findByIdAndTenant_IdAndIsDeletedFalse(eventId, organisation.getId())
        .orElseThrow(() -> new IllegalArgumentException("Event not found"));
    Tenant vendor = tenants.findById(UUID.fromString(body.getVendorId()))
        .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
    if (vendor.getTenantType() != Tenant.TenantType.VENDOR || !isLinked(organisation, vendor))
      throw new IllegalArgumentException("Vendor does not have an active relationship with this organisation");
    Set<String> groupMemberships = new HashSet<>();
    List<EventVendorTransportRequest> created = new ArrayList<>();
    for (EventVendorTransportRequestCreate.Group group : body.getGroups()) {
      String transportType = group.getTransportType().trim().toLowerCase(Locale.ROOT);
      List<EventParticipantRequest> selected = validateGroup(event, eventId, group, transportType);
      for (EventParticipantRequest participant : selected) {
        if (!groupMemberships.add(transportType + ":" + participant.getId()))
          throw new IllegalArgumentException("A participant cannot appear in more than one group for the same transport type");
      }
      EventVendorTransportRequest request = new EventVendorTransportRequest();
      request.setEvent(event); request.setVendor(vendor); request.setTransportType(transportType);
      request.setFormat(selected.size() == 1 ? EventVendorTransportRequest.RequestFormat.INDIVIDUAL
          : EventVendorTransportRequest.RequestFormat.GROUPED);
      request.setVehicleTypeId(UUID.fromString(group.getVehicleTypeId()));
      request.setDutyTypeId(UUID.fromString(group.getDutyTypeId()));
      request.setPickupTime(group.getPickupTime()); request.setParticipants(new ArrayList<>(selected));
      request.setStatus(EventVendorTransportRequest.RequestStatus.SENT);
      request.setCreatedBy(actorId == null ? null : actorId.toString());
      created.add(request);
    }
    return requests.saveAll(created).stream().map(EventVendorTransportRequestResponse::from).toList();
  }

  private List<EventParticipantRequest> validateGroup(TransportEvent event, UUID eventId,
      EventVendorTransportRequestCreate.Group group, String transportType) {
    if (!event.getTransportTypes().contains(transportType))
      throw new IllegalArgumentException("Transport type is not enabled for this event");
    if (!Set.of("venue-to-residency", "residency-to-venue", "round-trip").contains(transportType))
      throw new IllegalArgumentException("Unsupported event transport type");
    UUID vehicleTypeId = UUID.fromString(group.getVehicleTypeId());
    if (event.getVehicleTypeMappings().stream().noneMatch(v -> vehicleTypeId.equals(v.getVehicleType().getId())))
      throw new IllegalArgumentException("Vehicle type is not enabled for this event");
    UUID dutyTypeId = UUID.fromString(group.getDutyTypeId());
    if (event.getDutyTypes().stream().noneMatch(d -> dutyTypeId.equals(d.getId())))
      throw new IllegalArgumentException("Duty type is not enabled for this event");
    List<UUID> ids = group.getParticipantRequestIds();
    if (new HashSet<>(ids).size() != ids.size()) throw new IllegalArgumentException("Duplicate participant request IDs");
    List<EventParticipantRequest> selected = ids.stream().map(id -> participants
        .findByIdAndEvent_IdAndIsDeletedFalse(id, eventId)
        .orElseThrow(() -> new IllegalArgumentException("Participant request not found: " + id))).toList();
    int headcount = 0;
    for (EventParticipantRequest p : selected) {
      if (p.getStatus() != EventParticipantRequest.RequestStatus.APPROVED)
        throw new IllegalArgumentException("Only approved participants can be sent to a vendor");
      if (!p.getTransportTypes().contains(transportType))
        throw new IllegalArgumentException("Every participant must have selected the requested transport type");
      headcount += 1 + p.getExtraPassengers().size();
    }
    int capacity = event.getVehicleTypeMappings().stream()
        .filter(v -> vehicleTypeId.equals(v.getVehicleType().getId())).findFirst().orElseThrow().getAllowedPassenger();
    if (headcount > capacity) throw new IllegalArgumentException("Passenger count exceeds the selected vehicle type capacity");
    return selected;
  }

  @Transactional(readOnly = true)
  public List<EventVendorTransportRequestResponse> inbox(Tenant vendor) {
    requireVendor(vendor);
    return requests.findAllByVendor_IdAndIsDeletedFalseOrderByCreatedAtDesc(vendor.getId()).stream()
        .map(EventVendorTransportRequestResponse::from).toList();
  }

  @Transactional
  public EventVendorTransportRequestResponse decide(UUID id, Tenant vendor, boolean accepted, UUID actorId) {
    EventVendorTransportRequest request = findForVendor(id, vendor);
    if (request.getStatus() != EventVendorTransportRequest.RequestStatus.SENT)
      throw new IllegalArgumentException("Only sent requests can be accepted or declined");
    request.setStatus(accepted ? EventVendorTransportRequest.RequestStatus.ACCEPTED
        : EventVendorTransportRequest.RequestStatus.DECLINED);
    request.setUpdatedBy(actorId == null ? null : actorId.toString());
    return EventVendorTransportRequestResponse.from(requests.save(request));
  }

  @Transactional
  public EventVendorTransportRequestResponse allot(UUID id, Tenant vendor, EventVendorTransportAllotment body, UUID actorId) {
    EventVendorTransportRequest request = findForVendor(id, vendor);
    if (request.getStatus() != EventVendorTransportRequest.RequestStatus.ACCEPTED)
      throw new IllegalArgumentException("Accept the request before assigning a driver and vehicle");
    if (body.getDriverId() == null || body.getDriverId().isBlank() || body.getVehicleId() == null || body.getVehicleId().isBlank())
      throw new IllegalArgumentException("Both driverId and vehicleId are required");
    if (request.getEvent().getCity() == null || request.getEvent().getCity().isBlank())
      throw new IllegalArgumentException("Event city is required before creating its trip");
    List<EventParticipantRequest> ps = request.getParticipants();
    boolean venueToHome = request.getTransportType().equals("venue-to-residency");
    boolean homeToVenue = request.getTransportType().equals("residency-to-venue");
    boolean roundTrip = request.getTransportType().equals("round-trip");
    TripCreateRequestDTO dto = new TripCreateRequestDTO();
    dto.setTripCategory(Trip.TripCategory.EVENT); dto.setOrganisationId(request.getEvent().getTenant().getId().toString());
    dto.setVendorId(vendor.getId().toString()); dto.setCity(request.getEvent().getCity());
    dto.setVehicleTypeId(request.getVehicleTypeId().toString()); dto.setDutyTypeId(request.getDutyTypeId().toString());
    dto.setPickupTime(request.getPickupTime()); dto.setDriverId(body.getDriverId()); dto.setVehicleId(body.getVehicleId());
    dto.setNotes("Event: " + request.getEvent().getName() + " (" + request.getFormat().name().toLowerCase(Locale.ROOT) + ")");
    List<TripStopRequestDTO> stops = new ArrayList<>();
    int seq = 1;
    if (venueToHome) {
      stops.add(eventStop(request.getEvent(), seq++, TripStop.StopType.PICKUP));
      stops.addAll(homeStops(ps, seq, TripStop.StopType.DROP));
    } else if (homeToVenue) {
      stops.addAll(homeStops(ps, seq, TripStop.StopType.PICKUP));
      stops.add(eventStop(request.getEvent(), seq + ps.size(), TripStop.StopType.DROP));
    } else if (roundTrip) {
      stops.addAll(homeStops(ps, seq, TripStop.StopType.PICKUP));
      stops.add(eventStop(request.getEvent(), seq + ps.size(), TripStop.StopType.DROP));
      stops.add(eventStop(request.getEvent(), seq + ps.size() + 1, TripStop.StopType.PICKUP));
      stops.addAll(homeStops(ps, seq + ps.size() + 2, TripStop.StopType.DROP));
    }
    dto.setStops(stops);
    Trip trip = trips.createTrip(dto, vendor, actorId);
    TripAllotRequestDTO allot = new TripAllotRequestDTO(); allot.setDriverId(body.getDriverId()); allot.setVehicleId(body.getVehicleId());
    trip = trips.allotDriverVehicle(vendor, vendor.getId(), null, trip.getId(), allot);
    request.setTrip(trip); request.setStatus(EventVendorTransportRequest.RequestStatus.ALLOTTED);
    request.setUpdatedBy(actorId == null ? null : actorId.toString());
    return EventVendorTransportRequestResponse.from(requests.save(request));
  }

  private List<TripStopRequestDTO> homeStops(List<EventParticipantRequest> ps, int start, TripStop.StopType type) {
    List<TripStopRequestDTO> out = new ArrayList<>(); int seq = start;
    for (EventParticipantRequest p : ps) {
      TripStopRequestDTO s = new TripStopRequestDTO(); s.setSequenceNumber(seq++); s.setStopType(type);
      s.setAddressText(p.getPickupAddress() == null ? p.getName() : p.getPickupAddress());
      s.setLatitude(p.getPickupLatitude()); s.setLongitude(p.getPickupLongitude()); out.add(s);
    }
    return out;
  }
  private TripStopRequestDTO eventStop(TransportEvent e, int sequence, TripStop.StopType type) {
    TripStopRequestDTO s = new TripStopRequestDTO(); s.setSequenceNumber(sequence); s.setStopType(type);
    s.setAddressText(e.getVenue() + ", " + e.getCity()); s.setLatitude(e.getVenueLatitude()); s.setLongitude(e.getVenueLongitude()); return s;
  }
  private EventVendorTransportRequest findForVendor(UUID id, Tenant vendor) {
    requireVendor(vendor);
    return requests.findByIdAndVendor_IdAndIsDeletedFalse(id, vendor.getId())
        .orElseThrow(() -> new IllegalArgumentException("Event transport request not found"));
  }
  private void requireVendor(Tenant vendor) {
    if (vendor == null || vendor.getTenantType() != Tenant.TenantType.VENDOR) throw new IllegalArgumentException("Vendor tenant required");
  }
  private boolean isLinked(Tenant organisation, Tenant vendor) {
    boolean direct = vendorOrganisations.findByOrganisationAndVendor_Id(organisation, vendor.getId())
        .filter(link -> Boolean.TRUE.equals(link.getActive()) && link.getContractStatus() == VendorOrganisation.ContractStatus.ACTIVE).isPresent();
    if (direct) return true;
    return vendorPartners.findByPrimaryVendorAndPartnerVendor(organisation, vendor)
        .filter(link -> link.getContractStatus() == VendorPartner.ContractStatus.ACTIVE
            && Boolean.TRUE.equals(link.getIsActive()) && link.getServiceCategories().contains(VendorPartner.ServiceCategory.EVENTS)).isPresent()
        || vendorPartners.findByPrimaryVendorAndPartnerVendor(vendor, organisation)
        .filter(link -> link.getContractStatus() == VendorPartner.ContractStatus.ACTIVE
            && Boolean.TRUE.equals(link.getIsActive()) && link.getServiceCategories().contains(VendorPartner.ServiceCategory.EVENTS)).isPresent();
  }
}
