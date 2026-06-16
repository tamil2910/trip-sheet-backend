package com.example.trip_sheet_backend.services.PassengerService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripUpdateRequestDTO;
import com.example.trip_sheet_backend.models.PassengerOrganisationTenantMapping;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.PassengerOrganisationTenantMappingRepository;
import com.example.trip_sheet_backend.repositories.PeopleTenantRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.TripRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.services.TripService.TripServiceImp;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Service
@Transactional
public class PassengerServiceImp implements PassengerService {

    private final TenantRepository tenantRepository;
    private final UserAccountRepository userAccountRepository;
    private final PeopleTenantRepository peopleTenantRepository;
    private final TripRepository tripRepository;
    private final PassengerOrganisationTenantMappingRepository mappingRepository;
    private final TripServiceImp tripServiceImp;

    public PassengerServiceImp(
            TenantRepository tenantRepository,
            UserAccountRepository userAccountRepository,
            PeopleTenantRepository peopleTenantRepository,
            TripRepository tripRepository,
            PassengerOrganisationTenantMappingRepository mappingRepository,
            TripServiceImp tripServiceImp) {
        this.tenantRepository = tenantRepository;
        this.userAccountRepository = userAccountRepository;
        this.peopleTenantRepository = peopleTenantRepository;
        this.tripRepository = tripRepository;
        this.mappingRepository = mappingRepository;
        this.tripServiceImp = tripServiceImp;
    }

    @Override
    @Transactional(readOnly = true)
    public Tenant searchOrganisationByCode(String uniqueCode) {
        if (uniqueCode == null || uniqueCode.isBlank()) {
            throw new RuntimeException("uniqueCode is required");
        }

        Tenant organisation = tenantRepository.findByTenantUniqueCodeIgnoreCase(uniqueCode.trim())
                .orElseThrow(() -> new RuntimeException("Organisation not found"));

        if (organisation.getTenantType() != Tenant.TenantType.ORGANISATION) {
            throw new RuntimeException("Selected code does not belong to an organisation");
        }

        if (Boolean.FALSE.equals(organisation.getIsActive())) {
            throw new RuntimeException("Organisation is inactive");
        }

        return organisation;
    }

    @Override
    public void linkOrganisation(UserAccount user, String uniqueCode) {
        requireGuestUser(user);

        Tenant organisation = searchOrganisationByCode(uniqueCode);
        UUID userId = user.getId();
        UUID organisationId = organisation.getId();

        PassengerOrganisationTenantMapping mapping = mappingRepository
                .findByUserAccount_IdAndOrganisation_Id(userId, organisationId)
                .orElseGet(PassengerOrganisationTenantMapping::new);

        mapping.setUserAccount(user);
        mapping.setOrganisation(organisation);
        mapping.setIsActive(true);
        mapping.setLinkedAt(Instant.now().toEpochMilli());
        mapping.setUpdatedBy(userId == null ? null : userId.toString());
        if (mapping.getId() == null) {
            mapping.setCreatedBy(userId == null ? null : userId.toString());
        }
        mappingRepository.save(mapping);

        user.setTenant(organisation);
        userAccountRepository.save(user);
    }

    @Override
    public Trip createPassengerTrip(TripCreateRequestDTO dto, UserAccount user) {
        Tenant organisation = requireGuestOrganisation(user);

        if (dto == null) {
            throw new RuntimeException("Trip payload is required");
        }

        dto.setOrganisationId(organisation.getId().toString());
        dto.setIsManualTrip(false);

        return tripServiceImp.createTrip(dto, organisation, user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Trip> getMyTrips(UserAccount user, Map<String, Object> filters, Pageable pageable) {
        Tenant organisation = requireGuestOrganisation(user);
        Set<UUID> guestPeopleIds = resolveGuestPeopleIds(user, organisation.getId());

        if (guestPeopleIds.isEmpty()) {
            return Page.empty(pageable == null ? Pageable.unpaged() : pageable);
        }

        Specification<Trip> spec = buildGuestTripSpecification(organisation.getId(), guestPeopleIds, filters);
        return tripRepository.findAll(spec, pageable);
    }

    @Override
    public Trip updateMyTrip(UUID tripId, TripUpdateRequestDTO dto, UserAccount user) {
        Tenant organisation = requireGuestOrganisation(user);
        Trip trip = findAccessibleTrip(tripId, user, true, true);

        if (trip.getTripStatus() != Trip.TripStatus.CREATED) {
            throw new RuntimeException("Only CREATED trips can be updated");
        }

        if (dto == null) {
            throw new RuntimeException("Trip payload is required");
        }

        dto.setOrganisationId(organisation.getId().toString());
        dto.setIsManualTrip(false);

        return tripServiceImp.updateTrip(organisation.getId(), organisation, tripId, dto, user.getId());
    }

    @Override
    public void deleteMyTrip(UUID tripId, UserAccount user) {
        Tenant organisation = requireGuestOrganisation(user);
        Trip trip = findAccessibleTrip(tripId, user, false, true);

        if (trip.getTripStatus() != Trip.TripStatus.CREATED) {
            throw new RuntimeException("Only CREATED trips can be cancelled");
        }

        tripServiceImp.deleteTrip(organisation.getId(), tripId, user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Trip getTripDetails(UUID tripId, UserAccount user) {
        return findAccessibleTrip(tripId, user, false, true);
    }

    private Trip findAccessibleTrip(UUID tripId, UserAccount user, boolean requireBooker, boolean allowPassengerAccess) {
        Tenant organisation = requireGuestOrganisation(user);
        Set<UUID> guestPeopleIds = resolveGuestPeopleIds(user, organisation.getId());

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        if (Boolean.TRUE.equals(trip.getIsDeleted())) {
            throw new RuntimeException("Trip not found");
        }

        UUID tripOrganisationId = trip.getOrganisation() != null ? trip.getOrganisation().getId() : null;
        if (tripOrganisationId == null || !tripOrganisationId.equals(organisation.getId())) {
            throw new RuntimeException("ACCESS DENIED: Trip is not accessible for this organisation");
        }

        if (guestPeopleIds.isEmpty()) {
            throw new RuntimeException("ACCESS DENIED: Passenger profile not found");
        }

        UUID bookerId = trip.getBooker() != null ? trip.getBooker().getId() : null;
        boolean userIsBooker = bookerId != null && guestPeopleIds.contains(bookerId);
        boolean userIsPassenger = allowPassengerAccess
                && trip.getPassengers() != null
                && trip.getPassengers().stream().anyMatch(passenger -> passenger.getId() != null && guestPeopleIds.contains(passenger.getId()));

        if (requireBooker && !userIsBooker) {
            throw new RuntimeException("Only the booker can update this trip");
        }

        if (!userIsBooker && !userIsPassenger) {
            throw new RuntimeException("ACCESS DENIED: Trip is not accessible for this user");
        }

        return trip;
    }

    private Tenant requireGuestOrganisation(UserAccount user) {
        requireGuestUser(user);

        Tenant organisation = user.getTenant();
        if (organisation == null || organisation.getId() == null) {
            throw new RuntimeException("Guest organisation is not linked");
        }
        if (organisation.getTenantType() != Tenant.TenantType.ORGANISATION) {
            throw new RuntimeException("Guest organisation context is invalid");
        }
        return organisation;
    }

    private void requireGuestUser(UserAccount user) {
        if (user == null || user.getId() == null) {
            throw new RuntimeException("Authenticated user not found");
        }
    }

    private Set<UUID> resolveGuestPeopleIds(UserAccount user, UUID organisationId) {
        Set<UUID> guestPeopleIds = new LinkedHashSet<>();

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            List<PeopleTenant> peopleByEmail = peopleTenantRepository.findAllByEmailOrderByCreatedAtDesc(user.getEmail());
            for (PeopleTenant person : peopleByEmail) {
                if (person.getId() != null && person.getOrganisation() != null && organisationId.equals(person.getOrganisation().getId())) {
                    guestPeopleIds.add(person.getId());
                }
            }
        }

        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            peopleTenantRepository.findByPhoneAndOrganisation_Id(user.getPhone(), organisationId)
                    .map(PeopleTenant::getId)
                    .ifPresent(guestPeopleIds::add);
        }

        return guestPeopleIds;
    }

    private Specification<Trip> buildGuestTripSpecification(UUID organisationId, Set<UUID> guestPeopleIds, Map<String, Object> filters) {
        return (root, query, cb) -> {
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            predicates.add(cb.equal(root.join("organisation").get("id"), organisationId));

            Join<Object, Object> bookerJoin = root.join("booker", JoinType.LEFT);
            Join<Object, Object> passengersJoin = root.join("passengers", JoinType.LEFT);
            predicates.add(cb.or(
                    bookerJoin.get("id").in(guestPeopleIds),
                    passengersJoin.get("id").in(guestPeopleIds)
            ));

            applyGuestFilters(root, query, cb, predicates, filters);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void applyGuestFilters(
            jakarta.persistence.criteria.Root<Trip> root,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            List<Predicate> predicates,
            Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return;
        }

        Object globalSearchValue = filters.get("searchValue");
        if (globalSearchValue == null) {
            globalSearchValue = filters.get("globalSearch");
        }

        if (globalSearchValue != null && !globalSearchValue.toString().isBlank()) {
            String searchText = "%" + globalSearchValue.toString().trim().toLowerCase() + "%";
            List<Predicate> searchPredicates = new ArrayList<>();
            searchPredicates.add(cb.like(cb.lower(root.get("tripCode").as(String.class)), searchText));
            searchPredicates.add(cb.like(cb.lower(root.get("notes").as(String.class)), searchText));

            try {
                searchPredicates.add(cb.like(cb.lower(root.join("organisation", JoinType.LEFT).get("tenantName").as(String.class)), searchText));
            } catch (Exception ignored) {
            }
            try {
                searchPredicates.add(cb.like(cb.lower(root.join("booker", JoinType.LEFT).get("name").as(String.class)), searchText));
            } catch (Exception ignored) {
            }
            try {
                searchPredicates.add(cb.like(cb.lower(root.join("passengers", JoinType.LEFT).get("name").as(String.class)), searchText));
            } catch (Exception ignored) {
            }

            predicates.add(cb.or(searchPredicates.toArray(new Predicate[0])));
        }

        Object tripStatusValue = filters.get("tripStatus");
        if (tripStatusValue != null && !tripStatusValue.toString().isBlank()) {
            try {
                Trip.TripStatus tripStatus = Trip.TripStatus.valueOf(tripStatusValue.toString().trim());
                predicates.add(cb.equal(root.get("tripStatus"), tripStatus));
            } catch (Exception ignored) {
            }
        }

        Object tripTypeValue = filters.get("tripType");
        if (tripTypeValue != null && !tripTypeValue.toString().isBlank()) {
            try {
                Trip.TripType tripType = Trip.TripType.valueOf(tripTypeValue.toString().trim());
                predicates.add(cb.equal(root.get("tripType"), tripType));
            } catch (Exception ignored) {
            }
        }

        Object startDateValue = filters.get("startDate");
        if (startDateValue != null && !startDateValue.toString().isBlank()) {
            try {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), Long.parseLong(startDateValue.toString())));
            } catch (Exception ignored) {
            }
        }

        Object endDateValue = filters.get("endDate");
        if (endDateValue != null && !endDateValue.toString().isBlank()) {
            try {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), Long.parseLong(endDateValue.toString())));
            } catch (Exception ignored) {
            }
        }

        Object onlyCreatedValue = filters.get("onlyCreated");
        if (onlyCreatedValue != null && Boolean.parseBoolean(onlyCreatedValue.toString())) {
            predicates.add(cb.equal(root.get("tripStatus"), Trip.TripStatus.CREATED));
        }
    }
}
