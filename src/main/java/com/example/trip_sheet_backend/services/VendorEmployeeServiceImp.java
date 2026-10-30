package com.example.trip_sheet_backend.services;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.VendorEmployeeDtos.VendorEmployeeCreateRequestDto;
import com.example.trip_sheet_backend.dtos.VendorEmployeeDtos.VendorEmployeeResponseDto;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.RoleGroup;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.VendorEmployee;
import com.example.trip_sheet_backend.repositories.RoleGroupRepository;
import com.example.trip_sheet_backend.repositories.RoleRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.repositories.VendorEmployeeRepository;

@Service
public class VendorEmployeeServiceImp implements VendorEmployeeService {
    private final VendorEmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleGroupRepository roleGroupRepository;
    private final RoleRepository roleRepository;

    public VendorEmployeeServiceImp(
            VendorEmployeeRepository employeeRepository,
            UserAccountRepository userAccountRepository,
            RoleGroupRepository roleGroupRepository,
            RoleRepository roleRepository) {
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleGroupRepository = roleGroupRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VendorEmployeeResponseDto create(VendorEmployeeCreateRequestDto request, Tenant tenant, UUID createdBy) {
        if (tenant == null || tenant.getId() == null || tenant.getTenantType() != Tenant.TenantType.VENDOR) {
            throw new RuntimeException("A vendor tenant is required to create a vendor employee");
        }
        if (request.getIsUser() == null) {
            throw new RuntimeException("isUser is required");
        }

        String email = normalize(request.getEmail());
        String phone = normalize(request.getPhone());
        String employeeNumber = normalize(request.getEmployeeNumber());

        if (employeeNumber != null && employeeRepository.existsByTenant_IdAndEmployeeNumber(tenant.getId(), employeeNumber)) {
            throw new RuntimeException("Employee number already exists for this vendor");
        }

        VendorEmployee employee = new VendorEmployee();
        employee.setName(normalize(request.getName()));
        employee.setEmail(email);
        employee.setPhone(phone);
        employee.setEmployeeNumber(employeeNumber);
        employee.setIsUser(request.getIsUser());
        employee.setTenant(tenant);
        employee.setCreatedBy(createdBy == null ? null : createdBy.toString());
        employee.setUpdatedBy(createdBy == null ? null : createdBy.toString());

        if (Boolean.TRUE.equals(request.getIsUser())) {
            if (email == null && phone == null) {
                throw new RuntimeException("Email or phone is required when isUser is true");
            }
            if (email != null && userAccountRepository.existsByEmail(email)) {
                throw new RuntimeException("Email already belongs to a user account");
            }
            if (phone != null && userAccountRepository.existsByPhone(phone)) {
                throw new RuntimeException("Phone already belongs to a user account");
            }
            Set<UUID> requestedGroupIds = request.getRoleGroupIds() == null ? Set.of() : request.getRoleGroupIds();
            if (requestedGroupIds.isEmpty()) {
                throw new RuntimeException("At least one role group is required when isUser is true");
            }
            Set<RoleGroup> roleGroups = new HashSet<>(roleGroupRepository.findAllById(requestedGroupIds));
            if (roleGroups.size() != requestedGroupIds.size()
                    || roleGroups.stream().anyMatch(group -> group.getTenant() == null
                            || !tenant.getId().equals(group.getTenant().getId()))) {
                throw new RuntimeException("One or more role groups do not belong to this vendor");
            }

            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("USER role not found"));
            UserAccount account = new UserAccount();
            account.setUsername("vendor_employee_" + UUID.randomUUID().toString().replace("-", ""));
            account.setEmail(email);
            account.setPhone(phone);
            account.setLoginType(email != null ? UserAccount.LoginType.EMAIL : UserAccount.LoginType.PHONE);
            account.setRole(userRole);
            account.setTenant(tenant);
            account.setTenantType(UserAccount.TenantType.VENDOR);
            account.setIsActive(true);
            account.setRoleGroups(roleGroups);
            account.setCreatedBy(createdBy == null ? null : createdBy.toString());
            account.setUpdatedBy(createdBy == null ? null : createdBy.toString());
            employee.setAccount(userAccountRepository.save(account));
        } else if (request.getRoleGroupIds() != null && !request.getRoleGroupIds().isEmpty()) {
            throw new RuntimeException("Role groups can only be assigned when isUser is true");
        }

        return new VendorEmployeeResponseDto(employeeRepository.save(employee));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

