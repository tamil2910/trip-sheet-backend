package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.VendorEmployee;

public interface VendorEmployeeRepository extends BaseRepository<VendorEmployee, UUID> {
    boolean existsByTenant_IdAndEmployeeNumber(UUID tenantId, String employeeNumber);
    boolean existsByTenant_IdAndEmployeeNumberAndIdNot(UUID tenantId, String employeeNumber, UUID employeeId);
    Optional<VendorEmployee> findByAccount_IdAndIsUserTrue(UUID accountId);

    @EntityGraph(attributePaths = {"account", "account.roleGroups"})
    Optional<VendorEmployee> findByIdAndTenant_Id(UUID employeeId, UUID tenantId);

    @EntityGraph(attributePaths = {"account", "account.roleGroups"})
    @Query("""
        SELECT employee
        FROM VendorEmployee employee
        WHERE employee.tenant.id = :tenantId
          AND (:query IS NULL OR
               LOWER(COALESCE(employee.name, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(employee.email, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(employee.phone, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(employee.employeeNumber, '')) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (:name IS NULL OR LOWER(COALESCE(employee.name, '')) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:email IS NULL OR LOWER(COALESCE(employee.email, '')) LIKE LOWER(CONCAT('%', :email, '%')))
          AND (:phone IS NULL OR LOWER(COALESCE(employee.phone, '')) LIKE LOWER(CONCAT('%', :phone, '%')))
          AND (:employeeNumber IS NULL OR LOWER(COALESCE(employee.employeeNumber, '')) LIKE LOWER(CONCAT('%', :employeeNumber, '%')))
          AND (:isUser IS NULL OR employee.isUser = :isUser)
          AND employee.isDeleted = false
        """)
    Page<VendorEmployee> searchByTenant(
            @Param("tenantId") UUID tenantId,
            @Param("query") String query,
            @Param("name") String name,
            @Param("email") String email,
            @Param("phone") String phone,
            @Param("employeeNumber") String employeeNumber,
            @Param("isUser") Boolean isUser,
            Pageable pageable);
}
