package com.example.trip_sheet_backend.services;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.trip_sheet_backend.dtos.VehicleServiceRequest;
import com.example.trip_sheet_backend.exceptions.ResourceNotFoundException;
import com.example.trip_sheet_backend.models.*;
import com.example.trip_sheet_backend.repositories.*;

@Service @Transactional
public class VehicleServiceRecordService {
  private final VehicleServiceRecordRepository repository;
  private final VehicleRepository vehicleRepository;
  private final VehicleTenantMappingRepository vehicleTenantMappingRepository;
  private final DispatchCenterRepository dispatchCenterRepository;
  private final AuthorizedServiceCenterService centers;
  public VehicleServiceRecordService(VehicleServiceRecordRepository repository, VehicleRepository vehicleRepository, VehicleTenantMappingRepository vehicleTenantMappingRepository, DispatchCenterRepository dispatchCenterRepository, AuthorizedServiceCenterService centers) {
    this.repository=repository; this.vehicleRepository=vehicleRepository; this.vehicleTenantMappingRepository=vehicleTenantMappingRepository; this.dispatchCenterRepository=dispatchCenterRepository; this.centers=centers;
  }
  public List<VehicleServiceRecord> list(UUID tenantId) { return repository.findByTenant_IdAndIsDeletedFalseOrderByRequestDateDesc(tenantId); }
  public VehicleServiceRecord get(UUID tenantId, UUID id) { return repository.findByIdAndTenant_IdAndIsDeletedFalse(id, tenantId).orElseThrow(() -> new ResourceNotFoundException("Vehicle service not found")); }
  public VehicleServiceRecord create(UUID tenantId, VehicleServiceRequest body) { VehicleServiceRecord record = new VehicleServiceRecord(); record.setStatus(VehicleServiceRecord.ServiceStatus.PENDING); apply(record, tenantId, body); return repository.save(record); }
  public VehicleServiceRecord update(UUID tenantId, UUID id, VehicleServiceRequest body) { VehicleServiceRecord record=get(tenantId,id); apply(record,tenantId,body); return repository.save(record); }
  public VehicleServiceRecord updateStatus(UUID tenantId, UUID id, VehicleServiceRecord.ServiceStatus status) { VehicleServiceRecord record = get(tenantId, id); record.setStatus(status); return repository.save(record); }
  public void delete(UUID tenantId, UUID id) { VehicleServiceRecord record=get(tenantId,id); record.setIsDeleted(true); repository.save(record); }
  private void apply(VehicleServiceRecord r, UUID tenantId, VehicleServiceRequest b) {
    Vehicle vehicle=vehicleRepository.findById(b.getVehicleId()).orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
    if (!vehicleTenantMappingRepository.existsByVehicle_IdAndTenant_Id(vehicle.getId(), tenantId)) throw new ResourceNotFoundException("Vehicle not found for tenant");
    r.setTenant(tenant(tenantId)); r.setVehicle(vehicle); r.setServiceType(b.getServiceType());
    if (b.getAuthorizedServiceCenterId()!=null) { AuthorizedServiceCenter c=centers.get(tenantId,b.getAuthorizedServiceCenterId()); r.setAuthorizedServiceCenter(c); r.setCenterName(c.getName()); }
    else { r.setAuthorizedServiceCenter(null); r.setCenterName(b.getCenterName()); }
    r.setCallingPersonName(b.getCallingPersonName()); r.setPhoneNumber(b.getPhoneNumber());
    if (b.getDispatchCenterId() != null) {
      DispatchCenter dispatchCenter = dispatchCenterRepository.findByIdAndTenant_IdAndIsDeletedFalse(b.getDispatchCenterId(), tenantId)
          .orElseThrow(() -> new ResourceNotFoundException("Dispatch center not found for tenant"));
      r.setDispatchCenter(dispatchCenter);
    } else {
      r.setDispatchCenter(null);
    }
    r.setRequestDate(b.getRequestDate()); r.setEstimatedCompletionDate(b.getEstimatedCompletionDate()); r.setTakenInFor(b.getTakenInFor().trim());
    r.setOdometerKm(b.getOdometerKm()); r.setRequestNotes(b.getRequestNotes()); r.setEstimatedAmount(b.getEstimatedAmount()); r.setPaidAmount(b.getPaidAmount());
  }
  private static Tenant tenant(UUID id) { Tenant t=new Tenant(); t.setId(id); return t; }
}
