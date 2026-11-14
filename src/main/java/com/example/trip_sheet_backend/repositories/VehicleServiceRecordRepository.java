package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.trip_sheet_backend.models.VehicleServiceRecord;

public interface VehicleServiceRecordRepository extends JpaRepository<VehicleServiceRecord, UUID> {
  List<VehicleServiceRecord> findByTenant_IdAndIsDeletedFalseOrderByRequestDateDesc(UUID tenantId);
  Optional<VehicleServiceRecord> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);
  List<VehicleServiceRecord> findByTenant_IdAndIsDeletedFalseAndStatusOrderByRequestDateDesc(
      UUID tenantId, VehicleServiceRecord.ServiceStatus status);
  List<VehicleServiceRecord> findByTenant_IdAndIsDeletedFalseAndStatusAndEstimatedCompletionDateBeforeOrderByEstimatedCompletionDateAsc(
      UUID tenantId, VehicleServiceRecord.ServiceStatus status, Long date);

  @Query("""
      select count(v) from VehicleServiceRecord v
      where v.tenant.id = :tenantId and v.isDeleted = false and v.status <> :cancelled
        and (:fromDate is null or v.requestDate >= :fromDate)
        and (:toDate is null or v.requestDate <= :toDate)
      """)
  long countLogged(@Param("tenantId") UUID tenantId,
      @Param("cancelled") VehicleServiceRecord.ServiceStatus cancelled,
      @Param("fromDate") Long fromDate, @Param("toDate") Long toDate);

  @Query("""
      select coalesce(sum(v.paidAmount), :zero) from VehicleServiceRecord v
      where v.tenant.id = :tenantId and v.isDeleted = false and v.status <> :cancelled
        and (:fromDate is null or v.requestDate >= :fromDate)
        and (:toDate is null or v.requestDate <= :toDate)
      """)
  BigDecimal sumPaid(@Param("tenantId") UUID tenantId,
      @Param("cancelled") VehicleServiceRecord.ServiceStatus cancelled,
      @Param("fromDate") Long fromDate, @Param("toDate") Long toDate,
      @Param("zero") BigDecimal zero);

  @Query("""
      select coalesce(sum(case
        when v.estimatedAmount > coalesce(v.paidAmount, :zero)
        then v.estimatedAmount - coalesce(v.paidAmount, :zero)
        else :zero end), :zero)
      from VehicleServiceRecord v
      where v.tenant.id = :tenantId and v.isDeleted = false and v.status <> :cancelled
        and (:fromDate is null or v.requestDate >= :fromDate)
        and (:toDate is null or v.requestDate <= :toDate)
      """)
  BigDecimal sumUnsettled(@Param("tenantId") UUID tenantId,
      @Param("cancelled") VehicleServiceRecord.ServiceStatus cancelled,
      @Param("fromDate") Long fromDate, @Param("toDate") Long toDate,
      @Param("zero") BigDecimal zero);

  @Query("""
      select count(distinct v.vehicle.id) from VehicleServiceRecord v
      where v.tenant.id = :tenantId and v.isDeleted = false and v.status = :status
      """)
  long countByStatus(@Param("tenantId") UUID tenantId,
      @Param("status") VehicleServiceRecord.ServiceStatus status);

  @Query("""
      select count(distinct v.vehicle.id) from VehicleServiceRecord v
      where v.tenant.id = :tenantId and v.isDeleted = false and v.status = :status
        and v.estimatedCompletionDate is not null and v.estimatedCompletionDate < :date
      """)
  long countOverdue(@Param("tenantId") UUID tenantId,
      @Param("status") VehicleServiceRecord.ServiceStatus status, @Param("date") Long date);
}
