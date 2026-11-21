package com.example.trip_sheet_backend.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.TransportRequestRouteGroupDTO.RouteGroup;
import com.example.trip_sheet_backend.dtos.TransportRequestRouteGroupDTO.RouteLengthCategory;
import com.example.trip_sheet_backend.models.OrganisationSettings;
import com.example.trip_sheet_backend.models.OrganisationVendorAllocation;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.repositories.OrganisationSettingsRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;

@Service
public class EtsVendorAssignmentService {
  private final OrganisationSettingsRepository settingsRepository;
  private final VendorOrganisationRepository vendorOrganisationRepository;

  public EtsVendorAssignmentService(OrganisationSettingsRepository settingsRepository,
      VendorOrganisationRepository vendorOrganisationRepository) {
    this.settingsRepository = settingsRepository;
    this.vendorOrganisationRepository = vendorOrganisationRepository;
  }

  /** Adds non-persistent vendor suggestions to the analyzed route groups. */
  @Transactional(readOnly = true)
  public void suggestVendors(Tenant organisation, List<RouteGroup> groups) {
    if (organisation == null || groups == null || groups.isEmpty()) return;
    OrganisationSettings settings = settingsRepository
        .findFirstByTenant_IdAndIsDeletedFalseOrderByUpdatedAtDesc(organisation.getId()).orElse(null);

    List<Candidate> candidates = settings == null ? List.of() : configuredCandidates(settings);
    if (candidates.isEmpty()) candidates = linkedCandidates(organisation);
    if (candidates.isEmpty()) return;
    candidates = normalizeAndSort(candidates);

    Map<RouteLengthCategory, List<RouteGroup>> categories = new LinkedHashMap<>();
    for (RouteGroup group : groups) {
      RouteLengthCategory category = group.getRouteLengthCategory() == null
          ? RouteLengthCategory.LONGEST : group.getRouteLengthCategory();
      categories.computeIfAbsent(category, ignored -> new ArrayList<>()).add(group);
    }
    for (List<RouteGroup> categoryGroups : categories.values()) {
      assignCategory(categoryGroups, candidates);
    }
  }

  private List<Candidate> configuredCandidates(OrganisationSettings settings) {
    if (settings.getEtsVendorAllocations() == null || settings.getEtsVendorAllocations().isEmpty()) return List.of();
    return settings.getEtsVendorAllocations().stream()
        .filter(allocation -> !Boolean.TRUE.equals(allocation.getIsDeleted()))
        .map(this::toCandidate)
        .filter(candidate -> candidate != null && candidate.weight().compareTo(BigDecimal.ZERO) > 0)
        .toList();
  }

  private Candidate toCandidate(OrganisationVendorAllocation allocation) {
    VendorOrganisation link = allocation.getVendorOrganisation();
    if (!eligible(link)) return null;
    return new Candidate(link.getVendor(), allocation.getAllocationPercentage());
  }

  private List<Candidate> linkedCandidates(Tenant organisation) {
    return vendorOrganisationRepository.findByOrganisation(organisation).stream()
        .filter(this::eligible)
        .map(link -> new Candidate(link.getVendor(), BigDecimal.ONE))
        .toList();
  }

  private boolean eligible(VendorOrganisation link) {
    return link != null
        && !Boolean.TRUE.equals(link.getIsDeleted())
        && link.getVendor() != null
        && !Boolean.FALSE.equals(link.getVendor().getIsActive());
  }

  private List<Candidate> normalizeAndSort(List<Candidate> candidates) {
    List<Candidate> sorted = candidates.stream()
        .sorted(Comparator.comparing((Candidate c) -> c.vendor().getTenantName() == null
            ? "" : c.vendor().getTenantName(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(c -> c.vendor().getId()))
        .toList();
    BigDecimal total = sorted.stream().map(Candidate::weight).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.compareTo(BigDecimal.ZERO) <= 0) return List.of();
    return sorted.stream()
        .map(candidate -> new Candidate(candidate.vendor(), candidate.weight()
            .multiply(new BigDecimal("100" )).divide(total, 6, java.math.RoundingMode.HALF_UP)))
        .toList();
  }

  private void assignCategory(List<RouteGroup> groups, List<Candidate> candidates) {
    int count = groups.size();
    int[] remaining = new int[candidates.size()];
    double[] remainders = new double[candidates.size()];
    int assigned = 0;
    for (int i = 0; i < candidates.size(); i++) {
      double exact = count * candidates.get(i).weight().doubleValue() / 100.0;
      remaining[i] = (int) Math.floor(exact);
      remainders[i] = exact - remaining[i];
      assigned += remaining[i];
    }
    while (assigned < count) {
      int best = -1;
      for (int i = 0; i < candidates.size(); i++) {
        if (best < 0 || remainders[i] > remainders[best]) best = i;
      }
      remaining[best]++;
      remainders[best] = -1;
      assigned++;
    }

    double[] currentWeight = new double[candidates.size()];
    for (RouteGroup group : groups) {
      int selected = -1;
      for (int i = 0; i < candidates.size(); i++) {
        if (remaining[i] == 0) continue;
        currentWeight[i] += candidates.get(i).weight().doubleValue();
        if (selected < 0 || currentWeight[i] > currentWeight[selected]) selected = i;
      }
      Candidate candidate = candidates.get(selected);
      currentWeight[selected] -= 100.0;
      remaining[selected]--;
      group.setSuggestedVendorId(candidate.vendor().getId());
      group.setSuggestedVendorName(candidate.vendor().getTenantName());
      group.setSuggestedVendorAllocationPercentage(candidate.weight().doubleValue());
    }
  }

  private record Candidate(Tenant vendor, BigDecimal weight) { }
}
