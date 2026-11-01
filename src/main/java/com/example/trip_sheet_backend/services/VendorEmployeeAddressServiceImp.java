package com.example.trip_sheet_backend.services;

import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.VendorEmployeeAddressDtos.VendorEmployeeAddressRequestDto;
import com.example.trip_sheet_backend.dtos.VendorEmployeeAddressDtos.VendorEmployeeAddressResponseDto;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorEmployee;
import com.example.trip_sheet_backend.models.VendorEmployeeAddress;
import com.example.trip_sheet_backend.models.VendorEmployeeAddress.AddressCategory;
import com.example.trip_sheet_backend.repositories.VendorEmployeeAddressRepository;
import com.example.trip_sheet_backend.repositories.VendorEmployeeRepository;

@Service
public class VendorEmployeeAddressServiceImp implements VendorEmployeeAddressService {
    private final VendorEmployeeRepository employeeRepository;
    private final VendorEmployeeAddressRepository addressRepository;
    private final PassengerGeocodingService geocodingService;

    public VendorEmployeeAddressServiceImp(
            VendorEmployeeRepository employeeRepository,
            VendorEmployeeAddressRepository addressRepository,
            PassengerGeocodingService geocodingService) {
        this.employeeRepository = employeeRepository;
        this.addressRepository = addressRepository;
        this.geocodingService = geocodingService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VendorEmployeeAddressResponseDto create(
            Tenant tenant, UUID employeeId, UUID createdBy, VendorEmployeeAddressRequestDto request) {
        VendorEmployee employee = requireEmployee(tenant, employeeId);
        validateUniqueCategory(employeeId, request == null ? null : request.getCategory(), null);
        VendorEmployeeAddress address = new VendorEmployeeAddress();
        address.setVendorEmployee(employee);
        apply(address, request);
        address.setCreatedBy(createdBy == null ? null : createdBy.toString());
        address.setUpdatedBy(createdBy == null ? null : createdBy.toString());
        return new VendorEmployeeAddressResponseDto(addressRepository.save(address));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorEmployeeAddressResponseDto> list(Tenant tenant, UUID employeeId) {
        requireEmployee(tenant, employeeId);
        return addressRepository.findAllByVendorEmployee_IdAndIsDeletedFalse(employeeId).stream()
                .map(VendorEmployeeAddressResponseDto::new)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VendorEmployeeAddressResponseDto update(
            Tenant tenant, UUID employeeId, UUID addressId, UUID updatedBy, VendorEmployeeAddressRequestDto request) {
        requireEmployee(tenant, employeeId);
        VendorEmployeeAddress address = addressRepository
                .findByIdAndVendorEmployee_IdAndIsDeletedFalse(addressId, employeeId)
                .orElseThrow(() -> new RuntimeException("Address not found for this vendor employee"));
        validateUniqueCategory(employeeId, request == null ? null : request.getCategory(), addressId);
        apply(address, request);
        address.setUpdatedBy(updatedBy == null ? null : updatedBy.toString());
        return new VendorEmployeeAddressResponseDto(addressRepository.save(address));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Tenant tenant, UUID employeeId, UUID addressId) {
        requireEmployee(tenant, employeeId);
        VendorEmployeeAddress address = addressRepository
                .findByIdAndVendorEmployee_IdAndIsDeletedFalse(addressId, employeeId)
                .orElseThrow(() -> new RuntimeException("Address not found for this vendor employee"));
        addressRepository.delete(address);
    }

    private VendorEmployee requireEmployee(Tenant tenant, UUID employeeId) {
        if (tenant == null || tenant.getId() == null || tenant.getTenantType() != Tenant.TenantType.VENDOR) {
            throw new RuntimeException("Vendor tenant context is required");
        }
        return employeeRepository.findByIdAndTenant_Id(employeeId, tenant.getId())
                .orElseThrow(() -> new RuntimeException("Vendor employee not found for this tenant"));
    }

    private void validateUniqueCategory(UUID employeeId, AddressCategory category, UUID excludingAddressId) {
        if (category != AddressCategory.HOME && category != AddressCategory.OFFICE) {
            return;
        }
        boolean exists = excludingAddressId == null
                ? addressRepository.existsByVendorEmployee_IdAndCategoryAndIsDeletedFalse(employeeId, category)
                : addressRepository.existsByVendorEmployee_IdAndCategoryAndIdNotAndIsDeletedFalse(
                        employeeId, category, excludingAddressId);
        if (exists) {
            throw new RuntimeException("This vendor employee already has a " + category.name().toLowerCase() + " address");
        }
    }
    private void apply(VendorEmployeeAddress address, VendorEmployeeAddressRequestDto request) {
        if (request == null || request.getCategory() == null
                || request.getAddressLine1() == null || request.getAddressLine1().isBlank()) {
            throw new RuntimeException("Address category and addressLine1 are required");
        }
        address.setCategory(request.getCategory());
        address.setAddressLine1(request.getAddressLine1().trim());
        address.setAddressLine2(normalize(request.getAddressLine2()));
        address.setLandmark(normalize(request.getLandmark()));
        address.setCity(normalize(request.getCity()));
        address.setDistrict(normalize(request.getDistrict()));
        address.setState(normalize(request.getState()));
        address.setPostalCode(normalize(request.getPostalCode()));
        address.setCountry(normalize(request.getCountry()));
        String addressQuery = java.util.stream.Stream.of(
                address.getAddressLine1(), address.getAddressLine2(), address.getLandmark(),
                address.getCity(), address.getDistrict(), address.getState(),
                address.getPostalCode(), address.getCountry())
            .filter(value -> value != null && !value.isBlank())
            .collect(Collectors.joining(", "));
        PassengerGeocodingService.GeoPoint point = geocodingService.geocode(addressQuery);
        address.setLatitude(point.latitude());
        address.setLongitude(point.longitude());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
