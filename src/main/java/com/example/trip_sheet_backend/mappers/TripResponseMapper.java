package com.example.trip_sheet_backend.mappers;

import java.util.Collections;
import java.util.List;

import com.example.trip_sheet_backend.dtos.TripDtos.TripBasicRelationResponseDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripPassengerCustomFieldValueResponseDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripRelationResponseDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripResponseDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripStopResponseDTO;
import com.example.trip_sheet_backend.models.DispatchCenter;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.TripPassengerCustomFieldValue;
import com.example.trip_sheet_backend.models.TripStop;
import com.example.trip_sheet_backend.models.Vehicle;
import com.example.trip_sheet_backend.models.VehicleType;

public final class TripResponseMapper {

    private TripResponseMapper() {
    }

    public static TripResponseDTO toDTO(Trip trip) {
        TripResponseDTO dto = new TripResponseDTO();

        if (trip.getId() != null) {
            dto.setId(trip.getId().toString());
        }
        if (trip.getParentTrip() != null && trip.getParentTrip().getId() != null) {
            dto.setParentTripId(trip.getParentTrip().getId().toString());
        }
        if (trip.getTripSummary() != null && trip.getTripSummary().getId() != null) {
            dto.setTripSummaryId(trip.getTripSummary().getId().toString());
        }

        dto.setTripCode(trip.getTripCode());
        dto.setTripStatus(trip.getTripStatus());
        dto.setTripType(trip.getTripType());
        dto.setRecurrenceInterval(trip.getRecurrenceInterval());
        dto.setDaysOfWeek(trip.getDaysOfWeek());
        dto.setRecurrenceFrequency(trip.getRecurrenceFrequency());
        dto.setAirportTransferType(trip.getAirportTransferType());

        dto.setVendor(toTenantRelation(trip.getVendor()));
        dto.setOrganisation(toTenantRelation(trip.getOrganisation()));
        dto.setAssignedByVendor(toTenantRelation(trip.getAssignedByVendor()));
        dto.setPreviousVendor(toTenantRelation(trip.getPreviousVendor()));

        dto.setDriver(toDriverRelation(trip.getDriver()));
        dto.setVehicle(toVehicleRelation(trip.getVehicle()));
        dto.setDispatchCenter(toDispatchCenterRelation(trip.getDispatchCenter()));
        dto.setVehicleType(toVehicleTypeRelation(trip.getVehicleType()));
        dto.setDutyType(toDutyTypeRelation(trip.getDutyType()));
        dto.setBooker(toPeopleRelation(trip.getBooker()));

        if (trip.getPassengers() == null) {
            dto.setPassengers(Collections.emptyList());
        } else {
            dto.setPassengers(trip.getPassengers().stream()
                .map(TripResponseMapper::toPeopleRelation)
                .toList());
        }

            dto.setPassengerCustomFieldValues(toPassengerCustomFieldValueDTOs(trip.getPassengerCustomFieldValues()));

        dto.setNotes(trip.getNotes());
        dto.setPickupTime(trip.getPickupTime());
        dto.setStartDate(trip.getStartDate());
        dto.setEndDate(trip.getEndDate());
        dto.setStartOtp(trip.getStartOtp());
        dto.setEndOtp(trip.getEndOtp());
        dto.setIsManualTrip(trip.getIsManualTrip());

        dto.setStops(toStopDTOs(trip.getStops()));

        return dto;
    }

    private static List<TripStopResponseDTO> toStopDTOs(List<TripStop> stops) {
        if (stops == null) {
            return Collections.emptyList();
        }

        return stops.stream().map(stop -> {
            TripStopResponseDTO stopDTO = new TripStopResponseDTO();
            if (stop.getId() != null) {
                stopDTO.setId(stop.getId().toString());
            }
            stopDTO.setSequenceNumber(stop.getSequenceNumber());
            stopDTO.setStopType(stop.getStopType());
            stopDTO.setAddressText(stop.getAddressText());
            stopDTO.setFormattedAddress(stop.getFormattedAddress());
            stopDTO.setLatitude(stop.getLatitude());
            stopDTO.setLongitude(stop.getLongitude());
            stopDTO.setAccurate(stop.getAccurate());
            return stopDTO;
        }).toList();
    }

    private static List<TripPassengerCustomFieldValueResponseDTO> toPassengerCustomFieldValueDTOs(
        List<TripPassengerCustomFieldValue> values) {
        if (values == null) {
            return Collections.emptyList();
        }

        return values.stream().map(item -> {
            TripPassengerCustomFieldValueResponseDTO dto = new TripPassengerCustomFieldValueResponseDTO();
            if (item.getId() != null) {
                dto.setId(item.getId().toString());
            }
            dto.setPassenger(toPeopleRelation(item.getPassenger()));
            dto.setCustomField(toCustomFieldRelation(item.getCustomField()));
            dto.setValue(item.getValue());
            return dto;
        }).toList();
    }

    private static TripBasicRelationResponseDTO toCustomFieldRelation(com.example.trip_sheet_backend.models.CustomField customField) {
        if (customField == null) {
            return null;
        }

        TripBasicRelationResponseDTO ref = new TripBasicRelationResponseDTO();
        if (customField.getId() != null) {
            ref.setId(customField.getId().toString());
        }
        ref.setName(customField.getName());
        return ref;
    }

    private static TripRelationResponseDTO toTenantRelation(Tenant tenant) {
        if (tenant == null) {
            return null;
        }

        TripRelationResponseDTO ref = new TripRelationResponseDTO();
        if (tenant.getId() != null) {
            ref.setId(tenant.getId().toString());
        }
        ref.setName(tenant.getTenantName());
        ref.setPhone(null);
        return ref;
    }

    private static TripRelationResponseDTO toDriverRelation(Driver driver) {
        if (driver == null) {
            return null;
        }

        TripRelationResponseDTO ref = new TripRelationResponseDTO();
        if (driver.getId() != null) {
            ref.setId(driver.getId().toString());
        }
        ref.setName(driver.getFullName());
        String phone = null;
        if (driver.getAccount() != null) {
            phone = driver.getAccount().getPhone();
        }
        ref.setPhone(phone);
        return ref;
    }

    private static TripBasicRelationResponseDTO toVehicleRelation(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }

        TripBasicRelationResponseDTO ref = new TripBasicRelationResponseDTO();
        if (vehicle.getId() != null) {
            ref.setId(vehicle.getId().toString());
        }
        ref.setName(vehicle.getVehicleNumber());
        return ref;
    }

    private static TripBasicRelationResponseDTO toDispatchCenterRelation(DispatchCenter dispatchCenter) {
        if (dispatchCenter == null) {
            return null;
        }

        TripBasicRelationResponseDTO ref = new TripBasicRelationResponseDTO();
        if (dispatchCenter.getId() != null) {
            ref.setId(dispatchCenter.getId().toString());
        }
        ref.setName(dispatchCenter.getName());
        return ref;
    }

    private static TripBasicRelationResponseDTO toVehicleTypeRelation(VehicleType vehicleType) {
        if (vehicleType == null) {
            return null;
        }

        TripBasicRelationResponseDTO ref = new TripBasicRelationResponseDTO();
        if (vehicleType.getId() != null) {
            ref.setId(vehicleType.getId().toString());
        }
        ref.setName(vehicleType.getDefaultName());
        return ref;
    }

    private static TripBasicRelationResponseDTO toDutyTypeRelation(DutyType dutyType) {
        if (dutyType == null) {
            return null;
        }

        TripBasicRelationResponseDTO ref = new TripBasicRelationResponseDTO();
        if (dutyType.getId() != null) {
            ref.setId(dutyType.getId().toString());
        }
        ref.setName(dutyType.getName());
        return ref;
    }

    private static TripRelationResponseDTO toPeopleRelation(PeopleTenant people) {
        if (people == null) {
            return null;
        }

        TripRelationResponseDTO ref = new TripRelationResponseDTO();
        if (people.getId() != null) {
            ref.setId(people.getId().toString());
        }
        ref.setName(people.getName());
        ref.setPhone(people.getPhone());
        return ref;
    }
}
