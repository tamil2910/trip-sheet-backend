package com.example.trip_sheet_backend.services;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;
import java.io.IOException;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.example.trip_sheet_backend.dtos.VehicleServiceRequest;
import com.example.trip_sheet_backend.dtos.VehicleServiceExpenseItemRequest;
import com.example.trip_sheet_backend.dtos.VehicleServiceExpenseDtos.ItemCategoryResponse;
import com.example.trip_sheet_backend.dtos.VehicleServiceExpenseDtos.InvoiceResponse;
import com.example.trip_sheet_backend.dtos.VehicleServiceExpenseDtos.ItemResponse;
import com.example.trip_sheet_backend.dtos.VehicleServiceInvoiceRequest;
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
  private final VehicleServiceItemCategoryRepository itemCategories;
  private final VehicleServiceExpenseItemRepository expenseItems;
  private final VehicleServiceInvoiceRepository invoices;
  private final RailwayBucketStorageService bucketStorage;
  public VehicleServiceRecordService(VehicleServiceRecordRepository repository, VehicleRepository vehicleRepository,
      VehicleTenantMappingRepository vehicleTenantMappingRepository, DispatchCenterRepository dispatchCenterRepository,
      AuthorizedServiceCenterService centers, VehicleServiceItemCategoryRepository itemCategories,
      VehicleServiceExpenseItemRepository expenseItems, VehicleServiceInvoiceRepository invoices,
      RailwayBucketStorageService bucketStorage) {
    this.repository=repository; this.vehicleRepository=vehicleRepository;
    this.vehicleTenantMappingRepository=vehicleTenantMappingRepository;
    this.dispatchCenterRepository=dispatchCenterRepository; this.centers=centers;
    this.itemCategories=itemCategories; this.expenseItems=expenseItems; this.invoices=invoices;
    this.bucketStorage=bucketStorage;
  }
  public List<VehicleServiceRecord> list(UUID tenantId) { return repository.findByTenant_IdAndIsDeletedFalseOrderByRequestDateDesc(tenantId); }
  public VehicleServiceRecord get(UUID tenantId, UUID id) { return repository.findByIdAndTenant_IdAndIsDeletedFalse(id, tenantId).orElseThrow(() -> new ResourceNotFoundException("Vehicle service not found")); }
  public long servicesLogged(UUID tenantId, Long fromDate, Long toDate) { return repository.countLogged(tenantId, VehicleServiceRecord.ServiceStatus.CANCELLED, fromDate, toDate); }
  public BigDecimal totalSpent(UUID tenantId, Long fromDate, Long toDate) { return repository.sumPaid(tenantId, VehicleServiceRecord.ServiceStatus.CANCELLED, fromDate, toDate, BigDecimal.ZERO); }
  public BigDecimal unsettledBills(UUID tenantId, Long fromDate, Long toDate) { return repository.sumUnsettled(tenantId, VehicleServiceRecord.ServiceStatus.CANCELLED, fromDate, toDate, BigDecimal.ZERO); }
  public long inServiceCount(UUID tenantId) { return repository.countByStatus(tenantId, VehicleServiceRecord.ServiceStatus.IN_SERVICE); }
  public long pendingCount(UUID tenantId) { return repository.countByStatus(tenantId, VehicleServiceRecord.ServiceStatus.PENDING); }
  public List<VehicleServiceRecord> pendingList(UUID tenantId) { return repository.findByTenant_IdAndIsDeletedFalseAndStatusOrderByRequestDateDesc(tenantId, VehicleServiceRecord.ServiceStatus.PENDING); }
  public long overdueCount(UUID tenantId) { return repository.countOverdue(tenantId, VehicleServiceRecord.ServiceStatus.IN_SERVICE, Instant.now().toEpochMilli()); }
  public List<VehicleServiceRecord> overdueList(UUID tenantId) { return repository.findByTenant_IdAndIsDeletedFalseAndStatusAndEstimatedCompletionDateBeforeOrderByEstimatedCompletionDateAsc(tenantId, VehicleServiceRecord.ServiceStatus.IN_SERVICE, Instant.now().toEpochMilli()); }
  public VehicleServiceRecord create(UUID tenantId, VehicleServiceRequest body) {
    validateExpenseItems(body.getExpenseItems(), body.getPaidAmount(), List.of());
    VehicleServiceRecord record = new VehicleServiceRecord();
    record.setStatus(VehicleServiceRecord.ServiceStatus.PENDING);
    apply(record, tenantId, body);
    record = repository.save(record);
    if (body.getExpenseItems() != null) replaceExpenseItems(record, tenantId, body.getExpenseItems());
    if (body.getInvoices() != null && !body.getInvoices().isEmpty()) {
      uploadInvoicesBase64(tenantId, record.getId(), body.getInvoices());
    }
    return record;
  }
  public VehicleServiceRecord create(UUID tenantId, VehicleServiceRequest body, MultipartFile[] files) {
    VehicleServiceRecord record = create(tenantId, body);
    if (files != null && files.length > 0) uploadInvoices(tenantId, record.getId(), files);
    return record;
  }
  public VehicleServiceRecord update(UUID tenantId, UUID id, VehicleServiceRequest body) {
    VehicleServiceRecord record=get(tenantId,id);
    List<VehicleServiceExpenseItem> existingItems = expenseItems
        .findByVehicleService_IdAndVehicleService_Tenant_IdAndIsDeletedFalseOrderByCreatedAtAsc(id, tenantId);
    validateExpenseItems(body.getExpenseItems(), body.getPaidAmount(), existingItems);
    apply(record,tenantId,body);
    record = repository.save(record);
    if (body.getExpenseItems() != null) replaceExpenseItems(record, tenantId, body.getExpenseItems());
    if (body.getInvoices() != null && !body.getInvoices().isEmpty()) {
      uploadInvoicesBase64(tenantId, record.getId(), body.getInvoices());
    }
    return record;
  }
  public VehicleServiceRecord update(UUID tenantId, UUID id, VehicleServiceRequest body, MultipartFile[] files) {
    VehicleServiceRecord record = update(tenantId, id, body);
    if (files != null && files.length > 0) uploadInvoices(tenantId, id, files);
    return record;
  }
  public List<ItemCategoryResponse> itemCategories(UUID tenantId) {
    return itemCategories.findByTenant_IdAndIsDeletedFalseOrderByNameAsc(tenantId).stream()
        .map(category -> new ItemCategoryResponse(category.getId(), category.getName())).toList();
  }
  public List<ItemResponse> expenseItems(UUID tenantId, UUID serviceId) {
    get(tenantId, serviceId);
    return expenseItems.findByVehicleService_IdAndVehicleService_Tenant_IdAndIsDeletedFalseOrderByCreatedAtAsc(serviceId, tenantId)
        .stream().map(item -> {
          BigDecimal lineTotal = item.getUnitValue().multiply(BigDecimal.valueOf(item.getQuantity()));
          if (item.getItemType() == VehicleServiceExpenseItem.ItemType.DISCOUNT) lineTotal = lineTotal.negate();
          return new ItemResponse(item.getId(), item.getCategory() == null ? null : item.getCategory().getId(),
              item.getItemName(), item.getItemType().name(), item.getQuantity(), item.getUnitValue(), lineTotal);
        }).toList();
  }
  public List<InvoiceResponse> uploadInvoices(UUID tenantId, UUID serviceId, MultipartFile[] files) {
    VehicleServiceRecord record = get(tenantId, serviceId);
    if (record.getPaidAmount() == null || record.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("An invoice can be attached after a paid amount is recorded");
    }
    if (files == null || files.length == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one invoice file");
    List<VehicleServiceInvoice> saved = new java.util.ArrayList<>();
    for (MultipartFile file : files) {
      if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice files cannot be empty");
      String fileName = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "invoice"));
      if (fileName.contains("..")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid invoice file name");
      if (fileName.length() > 255) fileName = fileName.substring(fileName.length() - 255);
      try {
        String key = bucketStorage.storeFile("vehicle-service-invoices-" + serviceId, file.getBytes(), file.getContentType());
        VehicleServiceInvoice invoice = new VehicleServiceInvoice();
        invoice.setVehicleService(record);
        invoice.setFileKey(key);
        invoice.setFileName(fileName);
        invoice.setContentType(contentTypeForKey(key));
        invoice.setFileSize(file.getSize());
        saved.add(invoices.save(invoice));
      } catch (IOException exception) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read invoice file", exception);
      }
    }
    return saved.stream().map(this::toInvoiceResponse).toList();
  }
  public List<InvoiceResponse> uploadInvoicesBase64(UUID tenantId, UUID serviceId,
      List<VehicleServiceInvoiceRequest> files) {
    VehicleServiceRecord record = get(tenantId, serviceId);
    requirePaidAmountForInvoice(record);
    if (files == null || files.isEmpty()) throw new IllegalArgumentException("At least one invoice is required");
    List<VehicleServiceInvoice> saved = new java.util.ArrayList<>();
    for (VehicleServiceInvoiceRequest file : files) {
      String fileName = StringUtils.cleanPath(file.getFileName());
      if (fileName.contains("..")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid invoice file name");
      String data = file.getBase64Data().trim();
      String dataUri = data.startsWith("data:") || !StringUtils.hasText(file.getContentType())
          ? data : "data:" + file.getContentType() + ";base64," + data;
      String key = bucketStorage.storeBase64("vehicle-service-invoices-" + serviceId, dataUri);
      VehicleServiceInvoice invoice = new VehicleServiceInvoice();
      invoice.setVehicleService(record);
      invoice.setFileKey(key);
      invoice.setFileName(fileName);
      invoice.setContentType(contentTypeForKey(key));
      invoice.setFileSize(decodedBase64Size(data));
      saved.add(invoices.save(invoice));
    }
    return saved.stream().map(this::toInvoiceResponse).toList();
  }
  public List<InvoiceResponse> listInvoices(UUID tenantId, UUID serviceId) {
    get(tenantId, serviceId);
    return invoices.findByVehicleService_IdAndVehicleService_Tenant_IdAndIsDeletedFalseOrderByCreatedAtAsc(serviceId, tenantId)
        .stream().map(this::toInvoiceResponse).toList();
  }
  public VehicleServiceRecord updateStatus(UUID tenantId, UUID id, VehicleServiceRecord.ServiceStatus status) {
    VehicleServiceRecord record = get(tenantId, id);
    if (status == VehicleServiceRecord.ServiceStatus.COMPLETED) {
      List<VehicleServiceExpenseItem> savedItems = expenseItems
          .findByVehicleService_IdAndVehicleService_Tenant_IdAndIsDeletedFalseOrderByCreatedAtAsc(id, tenantId);
      if (savedItems.isEmpty()) {
        throw new IllegalArgumentException("Add expense items before completing the vehicle service");
      }
      validateExpenseItems(null, record.getPaidAmount(), savedItems);
      List<VehicleServiceInvoice> savedInvoices = invoices
          .findByVehicleService_IdAndVehicleService_Tenant_IdAndIsDeletedFalseOrderByCreatedAtAsc(id, tenantId);
      if (savedInvoices.isEmpty()) {
        throw new IllegalArgumentException("Attach at least one invoice before completing the vehicle service");
      }
    }
    record.setStatus(status);
    return repository.save(record);
  }
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
  private void validateExpenseItems(List<VehicleServiceExpenseItemRequest> requested, BigDecimal paidAmount,
      List<VehicleServiceExpenseItem> existing) {
    BigDecimal itemTotal;
    if (requested != null) {
      itemTotal = BigDecimal.ZERO;
      for (VehicleServiceExpenseItemRequest item : requested) {
        if (item == null || item.getValue() == null || item.getValue().compareTo(BigDecimal.ZERO) < 0) {
          throw new IllegalArgumentException("Each service expense item requires a non-negative value");
        }
        int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
        if (quantity < 1) throw new IllegalArgumentException("Item quantity must be at least 1");
        boolean discount = isDiscount(item);
        if (!discount && (item.getItemName() == null || item.getItemName().isBlank())) {
          throw new IllegalArgumentException("itemName is required for non-discount items");
        }
        if (item.getItemName() != null && item.getItemName().trim().length() > 180) {
          throw new IllegalArgumentException("itemName must not exceed 180 characters");
        }
        BigDecimal lineTotal = item.getValue().multiply(BigDecimal.valueOf(quantity));
        itemTotal = discount ? itemTotal.subtract(lineTotal) : itemTotal.add(lineTotal);
      }
    } else if (existing != null && !existing.isEmpty()) {
      itemTotal = totalOf(existing);
    } else {
      return;
    }
    if (itemTotal.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Discounts cannot exceed the total item value");
    }
    if (paidAmount == null || itemTotal.compareTo(paidAmount) != 0) {
      throw new IllegalArgumentException("paidAmount must equal the item total after discounts: " + itemTotal);
    }
  }

  private BigDecimal totalOf(List<VehicleServiceExpenseItem> items) {
    BigDecimal total = BigDecimal.ZERO;
    for (VehicleServiceExpenseItem item : items) {
      BigDecimal lineTotal = item.getUnitValue().multiply(BigDecimal.valueOf(item.getQuantity()));
      total = item.getItemType() == VehicleServiceExpenseItem.ItemType.DISCOUNT
          ? total.subtract(lineTotal) : total.add(lineTotal);
    }
    return total;
  }

  private void replaceExpenseItems(VehicleServiceRecord record, UUID tenantId,
      List<VehicleServiceExpenseItemRequest> requested) {
    expenseItems.deleteByVehicleService_IdAndVehicleService_Tenant_Id(record.getId(), tenantId);
    for (VehicleServiceExpenseItemRequest request : requested) {
      boolean discount = isDiscount(request);
      int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
      VehicleServiceExpenseItem item = new VehicleServiceExpenseItem();
      item.setVehicleService(record);
      item.setItemType(discount ? VehicleServiceExpenseItem.ItemType.DISCOUNT : VehicleServiceExpenseItem.ItemType.ITEM);
      item.setQuantity(quantity);
      item.setUnitValue(request.getValue());
      if (discount) {
        item.setItemName(StringUtils.hasText(request.getItemName()) ? request.getItemName().trim() : "Discount");
        item.setCategory(null);
      } else {
        VehicleServiceItemCategory category = findOrCreateItemCategory(tenantId, request.getItemName());
        item.setItemName(category.getName());
        item.setCategory(category);
      }
      expenseItems.save(item);
    }
  }

  private VehicleServiceItemCategory findOrCreateItemCategory(UUID tenantId, String itemName) {
    String name = itemName.trim();
    String normalizedName = name.toLowerCase(Locale.ROOT);
    VehicleServiceItemCategory category = itemCategories.findByTenant_IdAndNormalizedName(tenantId, normalizedName).orElse(null);
    if (category != null) {
      if (Boolean.TRUE.equals(category.getIsDeleted())) {
        category.setIsDeleted(false);
        category.setDeletedAt(null);
        category.setDeletedBy(null);
        category = itemCategories.save(category);
      }
      return category;
    }
    category = new VehicleServiceItemCategory();
    category.setTenant(tenant(tenantId));
    category.setName(name);
    category.setNormalizedName(normalizedName);
    return itemCategories.save(category);
  }

  private boolean isDiscount(VehicleServiceExpenseItemRequest request) {
    return Boolean.TRUE.equals(request.getDiscount())
        || (request.getItemName() != null && request.getItemName().trim().equalsIgnoreCase("discount"));
  }

  private InvoiceResponse toInvoiceResponse(VehicleServiceInvoice invoice) {
    return new InvoiceResponse(invoice.getId(), invoice.getFileName(), invoice.getContentType(),
        invoice.getFileSize(), bucketStorage.createDownloadUrl(invoice.getFileKey()));
  }

  private void requirePaidAmountForInvoice(VehicleServiceRecord record) {
    if (record.getPaidAmount() == null || record.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("An invoice can be attached after a paid amount is recorded");
    }
  }

  private long decodedBase64Size(String value) {
    int comma = value.indexOf(',');
    String encoded = value.startsWith("data:") && comma >= 0 ? value.substring(comma + 1) : value;
    try {
      return Base64.getDecoder().decode(encoded).length;
    } catch (IllegalArgumentException exception) {
      try {
        return Base64.getUrlDecoder().decode(encoded).length;
      } catch (IllegalArgumentException urlException) {
        return Base64.getMimeDecoder().decode(encoded).length;
      }
    }
  }

  private String contentTypeForKey(String key) {
    String normalized = key.toLowerCase(Locale.ROOT);
    if (normalized.endsWith(".pdf")) return "application/pdf";
    if (normalized.endsWith(".jpg") || normalized.endsWith(".jpeg")) return "image/jpeg";
    if (normalized.endsWith(".png")) return "image/png";
    if (normalized.endsWith(".gif")) return "image/gif";
    if (normalized.endsWith(".bmp")) return "image/bmp";
    return "image/webp";
  }
  private static Tenant tenant(UUID id) { Tenant t=new Tenant(); t.setId(id); return t; }
}
