package com.example.trip_sheet_backend.services;

import com.example.trip_sheet_backend.dtos.ChatBookingDraftResponse;
import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.models.*;
import com.example.trip_sheet_backend.repositories.*;
import com.example.trip_sheet_backend.services.TripService.TripServiceImp;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class ChatBookingService {
  private final RestClient openAi;
  private final String apiKey;
  private final String model;
  private final ObjectMapper mapper;
  private final ChatBookingDraftRepository drafts;
  private final TenantRepository tenants;
  private final PeopleTenantRepository people;
  private final DutyTypeRepository duties;
  private final VehicleTypeRepository vehicleTypes;
  private final TripServiceImp trips;

  public ChatBookingService(@Value("${openai.api-key:}") String apiKey,
      @Value("${openai.model:gpt-4o-mini}") String model, ObjectMapper mapper,
      ChatBookingDraftRepository drafts, TenantRepository tenants, PeopleTenantRepository people,
      DutyTypeRepository duties, VehicleTypeRepository vehicleTypes, TripServiceImp trips) {
    this.apiKey = apiKey; this.model = model; this.mapper = mapper; this.drafts = drafts;
    this.tenants = tenants; this.people = people; this.duties = duties;
    this.vehicleTypes = vehicleTypes; this.trips = trips;
    this.openAi = RestClient.builder().baseUrl("https://api.openai.com/v1").build();
  }

  @Transactional
  public ChatBookingDraftResponse createDraft(String message, Tenant tenant) {
    if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("OPENAI_API_KEY is not configured");
    JsonNode parsed = extract(message);
    List<String> questions = new ArrayList<>();
    String orgName = text(parsed, "organisation");
    String passengerName = text(parsed, "passenger");
    String dutyText = text(parsed, "duty");
    String vehicleText = text(parsed, "vehicle");
    String city = text(parsed, "city");
    String date = text(parsed, "date");
    String time = text(parsed, "time");

    Tenant organisation = resolveTenant(orgName, tenant, questions);
    PeopleTenant passenger = resolvePerson(passengerName, organisation, tenant, questions);
    DutyType duty = resolveDuty(dutyText, questions);
    VehicleType vehicle = resolveVehicle(vehicleText, questions);
    if (city == null) questions.add("Which city is the booking in?");
    if (date == null) questions.add("What date should I book?");
    if (time == null) questions.add("What pickup time should I use?");

    TripCreateRequestDTO request = new TripCreateRequestDTO();
    request.setTripType(Trip.TripType.SINGLE);
    request.setOrganisationId(organisation == null ? null : organisation.getId().toString());
    request.setPassengerIds(passenger == null ? List.of() : List.of(passenger.getId().toString()));
    request.setDutyTypeId(duty == null ? null : duty.getId().toString());
    request.setVehicleTypeId(vehicle == null ? null : vehicle.getId().toString());
    request.setCity(city);
    if (date != null && time != null) {
      try { request.setPickupTime(LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time)).atZone(ZoneId.of("Asia/Kolkata")).toEpochSecond()); }
      catch (RuntimeException e) { questions.add("Please provide a valid date and time (IST)."); }
    }
    request.setBookerId(passenger == null ? null : passenger.getId().toString());

    Map<String,Object> stored = new LinkedHashMap<>();
    stored.put("request", request);
    stored.put("booking", Map.of("organisationName", orgName == null ? "" : orgName,
      "passengerName", passengerName == null ? "" : passengerName,
      "dutyTypeName", duty == null ? (dutyText == null ? "" : dutyText) : duty.getName(),
      "vehicleTypeName", vehicle == null ? (vehicleText == null ? "" : vehicleText) : vehicle.getDefaultName(),
      "city", city == null ? "" : city, "pickupDate", date == null ? "" : date, "pickupTime", time == null ? "" : time));
    ChatBookingDraft draft = new ChatBookingDraft();
    draft.setTenant(tenant); draft.setRequestText(message);
    try { draft.setBookingJson(mapper.writeValueAsString(stored)); }
    catch (Exception e) { throw new IllegalStateException("Could not save booking draft", e); }
    draft.setExpiresAt(System.currentTimeMillis() + 30 * 60 * 1000L);
    drafts.save(draft);
    Map<?,?> booking = (Map<?,?>) stored.get("booking");
    ChatBookingDraftResponse.Booking display = new ChatBookingDraftResponse.Booking(
      (String)booking.get("organisationName"), (String)booking.get("passengerName"),
      (String)booking.get("dutyTypeName"), (String)booking.get("vehicleTypeName"),
      (String)booking.get("city"), (String)booking.get("pickupDate"), (String)booking.get("pickupTime"));
    return new ChatBookingDraftResponse(draft.getId(), draft.getStatus().name(), display, questions);
  }

  @Transactional
  public Trip confirm(UUID draftId, Tenant tenant, UUID createdBy) {
    ChatBookingDraft draft = drafts.findByIdAndTenantIdForUpdate(draftId, tenant.getId())
      .orElseThrow(() -> new IllegalArgumentException("Booking draft not found"));
    if (draft.getStatus() != ChatBookingDraft.Status.PENDING) throw new IllegalArgumentException("Draft is not pending");
    if (draft.getExpiresAt() == null || draft.getExpiresAt() < System.currentTimeMillis()) {
      draft.setStatus(ChatBookingDraft.Status.EXPIRED); drafts.save(draft);
      throw new IllegalArgumentException("Draft expired; please submit the booking again");
    }
    try {
      JsonNode root = mapper.readTree(draft.getBookingJson());
      TripCreateRequestDTO request = mapper.treeToValue(root.get("request"), TripCreateRequestDTO.class);
      if (request.getDutyTypeId() == null || request.getVehicleTypeId() == null ||
          request.getOrganisationId() == null || request.getPassengerIds() == null || request.getPassengerIds().isEmpty() ||
          request.getCity() == null || request.getPickupTime() == null)
        throw new IllegalArgumentException("Draft needs clarification and cannot be confirmed yet");
      Trip created = trips.createTrip(request, tenant, createdBy);
      draft.setStatus(ChatBookingDraft.Status.CONFIRMED); draft.setTripId(created.getId().toString()); drafts.save(draft);
      return created;
    } catch (IllegalArgumentException e) { throw e; }
    catch (Exception e) { throw new IllegalStateException("Could not confirm booking", e); }
  }

  private JsonNode extract(String message) {
    Map<String,Object> body = Map.of("model", model, "response_format", Map.of("type", "json_object"),
      "messages", List.of(Map.of("role", "system", "content", "Extract booking fields as JSON only with keys organisation, passenger, duty, vehicle, city, date, time. Use null for absent or ambiguous values. date must be YYYY-MM-DD and time HH:mm 24-hour local time. Do not invent details."), Map.of("role", "user", "content", message)));
    try {
      JsonNode result = openAi.post().uri("/chat/completions").header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey).body(body).retrieve().body(JsonNode.class);
      return mapper.readTree(result.path("choices").path(0).path("message").path("content").asText());
    } catch (Exception e) { throw new IllegalStateException("Could not interpret booking request with OpenAI", e); }
  }
  private static String text(JsonNode node, String key) { JsonNode n=node.get(key); return n == null || n.isNull() || n.asText().isBlank() ? null : n.asText().trim(); }
  private Tenant resolveTenant(String name, Tenant tokenTenant, List<String> q) {
    if (name == null && tokenTenant.getTenantType() == Tenant.TenantType.ORGANISATION) return tokenTenant;
    if (name == null) { q.add("Which organisation is this booking for?"); return null; }
    List<Tenant> found = tenants.findByTenantNameContainingIgnoreCase(name).stream()
      .filter(t -> t.getTenantType()==Tenant.TenantType.ORGANISATION)
      .filter(t -> tokenTenant.getTenantType()!=Tenant.TenantType.ORGANISATION || t.getId().equals(tokenTenant.getId())).toList();
    return unique(found, Tenant::getTenantName, "organisation '"+name+"'", q);
  }
  private PeopleTenant resolvePerson(String name, Tenant org, Tenant token, List<String> q) {
    if(name==null || org==null) { if(name==null) q.add("Who is the passenger?"); return null; }
    List<PeopleTenant> candidates=people.findByOrganisation_Id(org.getId(), Pageable.unpaged()).getContent().stream()
      .filter(p -> p.getName()!=null && p.getName().equalsIgnoreCase(name)).toList();
    if(token.getTenantType()==Tenant.TenantType.VENDOR) candidates=candidates.stream().filter(p -> p.getAttachedVendors().stream().anyMatch(v -> v.getId().equals(token.getId()))).toList();
    return unique(candidates, PeopleTenant::getName, "passenger '"+name+"'", q);
  }
  private DutyType resolveDuty(String text, List<String> q) {
    if(text==null){q.add("Which duty type should I use?");return null;}
    String lower=text.toLowerCase(Locale.ROOT);
    Integer hr=null;
    java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+)\\s*(?:hr|hour)").matcher(lower); if(m.find())hr=Integer.valueOf(m.group(1));
    final Integer requestedHours = hr;
    java.util.regex.Matcher kmMatcher=java.util.regex.Pattern.compile("(\\d+)\\s*km").matcher(lower);
    Integer requestedKm=kmMatcher.find()?Integer.valueOf(kmMatcher.group(1)):null;
    List<DutyType> all=duties.findAll().stream().filter(d -> d.getTypeOfDuty()==DutyType.typeDuty.LOCAL &&
      (requestedHours==null || Objects.equals(d.getHr(),requestedHours)) &&
      (requestedKm==null || Objects.equals(d.getKm(),requestedKm))).toList();
    return unique(all, d -> d.getName()==null ? Objects.toString(d.getHr(),"")+"hr local" : d.getName(), "local duty", q);
  }
  private VehicleType resolveVehicle(String name, List<String> q) {
    if(name==null){q.add("Which vehicle type should I use?");return null;}
    List<VehicleType> found=vehicleTypes.findAll().stream().filter(v -> v.getDefaultName()!=null && v.getDefaultName().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT))).toList();
    return unique(found, VehicleType::getDefaultName, "vehicle '"+name+"'", q);
  }
  private <T> T unique(List<T> found, java.util.function.Function<T,String> label, String what, List<String> q) {
    if(found.size()==1)return found.get(0);
    if(found.isEmpty()) q.add("I couldn't find "+what+". Please check the name or select a match.");
    else q.add("More than one "+what+" matched: "+found.stream().map(label).collect(Collectors.joining(", "))+". Which one do you mean?");
    return null;
  }
}
