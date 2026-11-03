package com.example.trip_sheet_backend.services;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.trip_sheet_backend.dtos.VendorEmployeeDtos.VendorEmployeeCreateRequestDto;
import com.example.trip_sheet_backend.dtos.VendorEmployeeDtos.VendorEmployeeResponseDto;
import com.example.trip_sheet_backend.dtos.VendorEmployeeDtos.VendorEmployeeUpdateRequestDto;
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
    private final PasswordEncoder passwordEncoder;

    public VendorEmployeeServiceImp(
            VendorEmployeeRepository employeeRepository,
            UserAccountRepository userAccountRepository,
            RoleGroupRepository roleGroupRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleGroupRepository = roleGroupRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VendorEmployeeResponseDto update(UUID employeeId, VendorEmployeeUpdateRequestDto request, Tenant tenant, UUID updatedBy) {
        if (tenant == null || tenant.getId() == null || tenant.getTenantType() != Tenant.TenantType.VENDOR) {
            throw new RuntimeException("A vendor tenant is required to update a vendor employee");
        }
        if (request == null) {
            throw new RuntimeException("Update request is required");
        }
        VendorEmployee employee = employeeRepository.findByIdAndTenant_Id(employeeId, tenant.getId())
                .orElseThrow(() -> new RuntimeException("Vendor employee not found for this tenant"));

        if (request.getName() != null) employee.setName(normalize(request.getName()));
        if (request.getEmail() != null) employee.setEmail(normalize(request.getEmail()));
        if (request.getPhone() != null) employee.setPhone(normalize(request.getPhone()));
        if (request.getEmployeeNumber() != null) {
            String employeeNumber = normalize(request.getEmployeeNumber());
            if (employeeNumber != null && employeeRepository.existsByTenant_IdAndEmployeeNumberAndIdNot(
                    tenant.getId(), employeeNumber, employeeId)) {
                throw new RuntimeException("Employee number already exists for this vendor");
            }
            employee.setEmployeeNumber(employeeNumber);
        }

        boolean makeUser = request.getIsUser() == null
                ? Boolean.TRUE.equals(employee.getIsUser())
                : request.getIsUser();
        if (!makeUser && employee.getAccount() != null) {
            throw new RuntimeException("A linked user account cannot be removed through this update API");
        }
        if (!makeUser && request.getPassword() != null && !request.getPassword().isBlank()) {
            throw new RuntimeException("Password can only be updated for a user account");
        }
        if (!makeUser && request.getRoleGroupIds() != null && !request.getRoleGroupIds().isEmpty()) {
            throw new RuntimeException("Role groups can only be assigned when isUser is true");
        }

        if (makeUser) {
            String email = employee.getEmail();
            String phone = employee.getPhone();
            if (email == null && phone == null) {
                throw new RuntimeException("Email or phone is required when isUser is true");
            }
            validateAccountIdentity(email, phone, employee.getAccount());

            Set<UUID> requestedGroupIds = request.getRoleGroupIds();
            if (employee.getAccount() == null && (requestedGroupIds == null || requestedGroupIds.isEmpty())) {
                throw new RuntimeException("At least one role group is required when isUser is true");
            }
            if (requestedGroupIds != null) {
                if (requestedGroupIds.isEmpty()) {
                    throw new RuntimeException("At least one role group is required when isUser is true");
                }
                Set<RoleGroup> roleGroups = new HashSet<>(roleGroupRepository.findAllById(requestedGroupIds));
                if (roleGroups.size() != requestedGroupIds.size()
                        || roleGroups.stream().anyMatch(group -> group.getTenant() == null
                                || !tenant.getId().equals(group.getTenant().getId()))) {
                    throw new RuntimeException("One or more role groups do not belong to this vendor");
                }
                if (employee.getAccount() != null) employee.getAccount().setRoleGroups(roleGroups);
            }

            UserAccount account = employee.getAccount();
            if (account == null) {
                Role userRole = roleRepository.findByName("USER")
                        .orElseThrow(() -> new RuntimeException("USER role not found"));
                account = new UserAccount();
                account.setUsername("vendor_employee_" + UUID.randomUUID().toString().replace("-", ""));
                account.setRole(userRole);
                account.setTenant(tenant);
                account.setTenantType(UserAccount.TenantType.VENDOR);
                account.setIsActive(true);
                account.setRoleGroups(new HashSet<>(roleGroupRepository.findAllById(requestedGroupIds)));
                account.setCreatedBy(updatedBy == null ? null : updatedBy.toString());
                account.setCreatedByUser(updatedBy == null ? null : userAccountRepository.findById(updatedBy).orElse(null));
            }
            account.setEmail(email);
            account.setPhone(phone);
            account.setLoginType(email != null ? UserAccount.LoginType.EMAIL : UserAccount.LoginType.PHONE);
            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                account.setPassword(passwordEncoder.encode(request.getPassword()));
            }
            account.setUpdatedBy(updatedBy == null ? null : updatedBy.toString());
            employee.setAccount(userAccountRepository.save(account));
        }

        employee.setIsUser(makeUser);
        employee.setUpdatedBy(updatedBy == null ? null : updatedBy.toString());
        return new VendorEmployeeResponseDto(employeeRepository.save(employee));
    }

    private void validateAccountIdentity(String email, String phone, UserAccount currentAccount) {
        if (email != null) {
            UserAccount existing = userAccountRepository.findByEmail(email).orElse(null);
            if (existing != null && (currentAccount == null || !existing.getId().equals(currentAccount.getId()))) {
                throw new RuntimeException("Email already belongs to another user account");
            }
        }
        if (phone != null) {
            UserAccount existing = userAccountRepository.findByPhone(phone).orElse(null);
            if (existing != null && (currentAccount == null || !existing.getId().equals(currentAccount.getId()))) {
                throw new RuntimeException("Phone already belongs to another user account");
            }
        }
    }
    @Override
    @Transactional(readOnly = true)
    public VendorEmployeeResponseDto getById(Tenant tenant, UUID employeeId) {
        if (tenant == null || tenant.getId() == null || tenant.getTenantType() != Tenant.TenantType.VENDOR) {
            throw new RuntimeException("A vendor tenant is required to retrieve a vendor employee");
        }
        VendorEmployee employee = employeeRepository.findByIdAndTenant_Id(employeeId, tenant.getId())
                .orElseThrow(() -> new RuntimeException("Vendor employee not found for this tenant"));
        return new VendorEmployeeResponseDto(employee);
    }
    @Override
    @Transactional(readOnly = true)
    public Page<VendorEmployeeResponseDto> search(
            Tenant tenant,
            String query,
            String name,
            String email,
            String phone,
            String employeeNumber,
            Boolean isUser,
            Pageable pageable) {
        if (tenant == null || tenant.getId() == null || tenant.getTenantType() != Tenant.TenantType.VENDOR) {
            throw new RuntimeException("A vendor tenant is required to search vendor employees");
        }
        return employeeRepository.searchByTenant(
                tenant.getId(),
                normalize(query),
                normalize(name),
                normalize(email),
                normalize(phone),
                normalize(employeeNumber),
                isUser,
                pageable)
            .map(VendorEmployeeResponseDto::new);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}



