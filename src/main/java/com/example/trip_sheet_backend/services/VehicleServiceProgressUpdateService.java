package com.example.trip_sheet_backend.services;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.example.trip_sheet_backend.dtos.VehicleServiceProgressUpdateDtos.Request;
import com.example.trip_sheet_backend.dtos.VehicleServiceProgressUpdateDtos.Response;
import com.example.trip_sheet_backend.exceptions.ResourceNotFoundException;
import com.example.trip_sheet_backend.models.VehicleServiceProgressUpdate;
import com.example.trip_sheet_backend.models.VehicleServiceRecord;
import com.example.trip_sheet_backend.repositories.VehicleServiceProgressUpdateRepository;
import com.example.trip_sheet_backend.repositories.VehicleServiceRecordRepository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@Transactional
public class VehicleServiceProgressUpdateService {
  private final VehicleServiceProgressUpdateRepository updates;
  private final VehicleServiceRecordRepository services;
  private final RailwayBucketStorageService bucketStorage;

  public VehicleServiceProgressUpdateService(VehicleServiceProgressUpdateRepository updates,
      VehicleServiceRecordRepository services, RailwayBucketStorageService bucketStorage) {
    this.updates = updates;
    this.services = services;
    this.bucketStorage = bucketStorage;
  }

  public Response create(UUID tenantId, UUID serviceId, String userName, Request request) {
    VehicleServiceRecord service = serviceRecord(tenantId, serviceId);
    requireInService(service);
    VehicleServiceProgressUpdate update = new VehicleServiceProgressUpdate();
    update.setVehicleService(service);
    update.setDescription(request.description().trim());
    update.setUpdatedByName(userName);
    update.setCreatedBy(userName);
    update.setUpdatedBy(userName);
    update.setUpdatedAt(Instant.now().toEpochMilli());
    update = updates.save(update);
    if (StringUtils.hasText(request.photoBase64())) {
      update.setPhotoKey(storePhoto(update.getId(), request.photoBase64(), request.photoContentType()));
      update = updates.save(update);
    }
    return toResponse(update);
  }

  public Response update(UUID tenantId, UUID serviceId, UUID updateId, String userName, Request request) {
    VehicleServiceRecord service = serviceRecord(tenantId, serviceId);
    requireInService(service);
    VehicleServiceProgressUpdate update = findUpdate(tenantId, serviceId, updateId);
    update.setDescription(request.description().trim());
    update.setUpdatedByName(userName);
    update.setUpdatedBy(userName);
    update.setUpdatedAt(Instant.now().toEpochMilli());
    if (StringUtils.hasText(request.photoBase64())) {
      update.setPhotoKey(storePhoto(update.getId(), request.photoBase64(), request.photoContentType()));
    }
    return toResponse(updates.save(update));
  }

  @Transactional(readOnly = true)
  public List<Response> list(UUID tenantId, UUID serviceId) {
    serviceRecord(tenantId, serviceId);
    return updates.findByVehicleService_IdAndVehicleService_Tenant_IdAndIsDeletedFalseOrderByCreatedAtAsc(serviceId, tenantId)
        .stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public Response get(UUID tenantId, UUID serviceId, UUID updateId) {
    serviceRecord(tenantId, serviceId);
    return toResponse(findUpdate(tenantId, serviceId, updateId));
  }

  public void delete(UUID tenantId, UUID serviceId, UUID updateId, String userName) {
    serviceRecord(tenantId, serviceId);
    VehicleServiceProgressUpdate update = findUpdate(tenantId, serviceId, updateId);
    update.setIsDeleted(true);
    update.setDeletedAt(Instant.now().toEpochMilli());
    update.setDeletedBy(userName);
    update.setUpdatedByName(userName);
    update.setUpdatedBy(userName);
    update.setUpdatedAt(Instant.now().toEpochMilli());
    updates.save(update);
  }

  private VehicleServiceRecord serviceRecord(UUID tenantId, UUID serviceId) {
    return services.findByIdAndTenant_IdAndIsDeletedFalse(serviceId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Vehicle service not found"));
  }

  private void requireInService(VehicleServiceRecord service) {
    if (service.getStatus() != VehicleServiceRecord.ServiceStatus.IN_SERVICE) {
      throw new IllegalArgumentException("Progress updates can only be changed while the vehicle service is IN_SERVICE");
    }
  }

  private VehicleServiceProgressUpdate findUpdate(UUID tenantId, UUID serviceId, UUID updateId) {
    return updates.findByIdAndVehicleService_IdAndVehicleService_Tenant_IdAndIsDeletedFalse(updateId, serviceId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Vehicle service progress update not found"));
  }

  private String storePhoto(UUID updateId, String value, String declaredContentType) {
    String data = value.trim();
    String contentType = declaredContentType;
    if (data.startsWith("data:")) {
      int semicolon = data.indexOf(';');
      int comma = data.indexOf(',');
      if (semicolon > 5 && comma > semicolon) contentType = data.substring(5, semicolon);
    }
    if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "photoContentType must be an image type");
    }
    String dataUri = data.startsWith("data:") ? data : "data:" + contentType + ";base64," + data;
    return bucketStorage.storeBase64("vehicle-service-updates-" + updateId, dataUri);
  }

  private Response toResponse(VehicleServiceProgressUpdate update) {
    String photoUrl = update.getPhotoKey() == null ? null : bucketStorage.createDownloadUrl(update.getPhotoKey());
    return new Response(update.getId(), update.getVehicleService().getId(), update.getDescription(), photoUrl,
        update.getUpdatedByName(), update.getUpdatedAt());
  }
}
