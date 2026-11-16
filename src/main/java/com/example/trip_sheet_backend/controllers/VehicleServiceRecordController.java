package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.trip_sheet_backend.dtos.VehicleServiceRequest;
import com.example.trip_sheet_backend.dtos.VehicleServiceExpenseDtos.ItemCategoryResponse;
import com.example.trip_sheet_backend.dtos.VehicleServiceExpenseDtos.InvoiceResponse;
import com.example.trip_sheet_backend.dtos.VehicleServiceExpenseDtos.ItemResponse;
import com.example.trip_sheet_backend.dtos.VehicleServiceExpenseDtos.InvoiceUploadRequest;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.VehicleServiceRecord;
import com.example.trip_sheet_backend.dtos.VehicleServiceProgressUpdateDtos.Request;
import com.example.trip_sheet_backend.dtos.VehicleServiceProgressUpdateDtos.Response;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.VehicleServiceRecordService;
import com.example.trip_sheet_backend.services.VehicleServiceProgressUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/vehicle-services")
public class VehicleServiceRecordController {
  private final VehicleServiceRecordService service;
  private final VehicleServiceProgressUpdateService progressUpdates;
  public VehicleServiceRecordController(VehicleServiceRecordService service,
      VehicleServiceProgressUpdateService progressUpdates) { this.service=service; this.progressUpdates=progressUpdates; }
  @GetMapping @PreAuthorize("hasAuthority('CAN_READ_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<List<VehicleServiceRecord>>> list(HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"Vehicle services fetched successfully",service.list(tenantId(req)))); }
  @GetMapping("/metrics/services-logged") @PreAuthorize("hasAuthority('CAN_READ_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<Long>> servicesLogged(@RequestParam(required=false) Long fromDate,@RequestParam(required=false) Long toDate,HttpServletRequest req) { validateDateRange(fromDate,toDate); return ResponseEntity.ok(new ApiResponse<>(true,"Services logged count fetched successfully",service.servicesLogged(tenantId(req),fromDate,toDate))); }
  @GetMapping("/metrics/total-spent") @PreAuthorize("hasAuthority('CAN_READ_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<BigDecimal>> totalSpent(@RequestParam(required=false) Long fromDate,@RequestParam(required=false) Long toDate,HttpServletRequest req) { validateDateRange(fromDate,toDate); return ResponseEntity.ok(new ApiResponse<>(true,"Total spent fetched successfully",service.totalSpent(tenantId(req),fromDate,toDate))); }
  @GetMapping("/metrics/unsettled-bills") @PreAuthorize("hasAuthority('CAN_READ_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<BigDecimal>> unsettledBills(@RequestParam(required=false) Long fromDate,@RequestParam(required=false) Long toDate,HttpServletRequest req) { validateDateRange(fromDate,toDate); return ResponseEntity.ok(new ApiResponse<>(true,"Unsettled bills total fetched successfully",service.unsettledBills(tenantId(req),fromDate,toDate))); }
  @GetMapping("/metrics/in-service-count") @PreAuthorize("hasAuthority('CAN_READ_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<Long>> inServiceCount(HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"In-service vehicle count fetched successfully",service.inServiceCount(tenantId(req)))); }
  @GetMapping("/metrics/pending-count") @PreAuthorize("hasAuthority('CAN_READ_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<Long>> pendingCount(HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"Pending vehicle count fetched successfully",service.pendingCount(tenantId(req)))); }
  @GetMapping("/pending") @PreAuthorize("hasAuthority('CAN_READ_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<List<VehicleServiceRecord>>> pendingList(HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"Pending vehicle services fetched successfully",service.pendingList(tenantId(req)))); }
  @GetMapping("/metrics/overdue-count") @PreAuthorize("hasAuthority('CAN_READ_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<Long>> overdueCount(HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"Overdue vehicle count fetched successfully",service.overdueCount(tenantId(req)))); }
  @GetMapping("/overdue") @PreAuthorize("hasAuthority('CAN_READ_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<List<VehicleServiceRecord>>> overdueList(HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"Overdue vehicle services fetched successfully",service.overdueList(tenantId(req)))); }
  @GetMapping("/item-categories") @PreAuthorize("hasAuthority('CAN_READ_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<List<ItemCategoryResponse>>> itemCategories(HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"Vehicle service item categories fetched successfully",service.itemCategories(tenantId(req)))); }
  @GetMapping("/{id}/items") @PreAuthorize("hasAuthority('CAN_READ_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<List<ItemResponse>>> expenseItems(@PathVariable UUID id,HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"Vehicle service expense items fetched successfully",service.expenseItems(tenantId(req),id))); }
  @GetMapping("/{id}/invoices") @PreAuthorize("hasAuthority('CAN_READ_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<List<InvoiceResponse>>> invoices(@PathVariable UUID id,HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"Vehicle service invoices fetched successfully",service.listInvoices(tenantId(req),id))); }
  @GetMapping("/{id}/reports-updation") @PreAuthorize("hasAuthority('CAN_READ_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<List<Response>>> progressUpdates(@PathVariable UUID id,HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"Vehicle service progress updates fetched successfully",progressUpdates.list(tenantId(req),id))); }
  @GetMapping("/{id}/reports-updation/{updateId}") @PreAuthorize("hasAuthority('CAN_READ_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<Response>> progressUpdate(@PathVariable UUID id,@PathVariable UUID updateId,HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"Vehicle service progress update fetched successfully",progressUpdates.get(tenantId(req),id,updateId))); }
  @PostMapping(value = "/{id}/reports-updation", consumes = MediaType.APPLICATION_JSON_VALUE) @PreAuthorize("hasAuthority('CAN_UPDATE_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<Response>> createProgressUpdate(@PathVariable UUID id,@Valid @RequestBody Request body,HttpServletRequest req) { return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true,"Vehicle service progress update created successfully",progressUpdates.create(tenantId(req),id,userName(req),body))); }
  @PutMapping(value = "/{id}/reports-updation/{updateId}", consumes = MediaType.APPLICATION_JSON_VALUE) @PreAuthorize("hasAuthority('CAN_UPDATE_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<Response>> updateProgressUpdate(@PathVariable UUID id,@PathVariable UUID updateId,@Valid @RequestBody Request body,HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"Vehicle service progress update updated successfully",progressUpdates.update(tenantId(req),id,updateId,userName(req),body))); }
  @DeleteMapping("/{id}/reports-updation/{updateId}") @PreAuthorize("hasAuthority('CAN_DELETE_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<Void>> deleteProgressUpdate(@PathVariable UUID id,@PathVariable UUID updateId,HttpServletRequest req) { progressUpdates.delete(tenantId(req),id,updateId,userName(req)); return ResponseEntity.ok(new ApiResponse<>(true,"Vehicle service progress update deleted successfully",null)); }
  @GetMapping("/{id}") @PreAuthorize("hasAuthority('CAN_READ_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<VehicleServiceRecord>> get(@PathVariable UUID id,HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"Vehicle service fetched successfully",service.get(tenantId(req),id))); }
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE) @PreAuthorize("hasAuthority('CAN_CREATE_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<VehicleServiceRecord>> create(@Valid @RequestBody VehicleServiceRequest body,HttpServletRequest req) { return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true,"Vehicle service created successfully",service.create(tenantId(req),body))); }
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasAuthority('CAN_CREATE_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<VehicleServiceRecord>> createWithInvoices(
      @RequestPart("service") @Valid VehicleServiceRequest body,
      @RequestPart(value = "invoices", required = false) MultipartFile[] files,
      HttpServletRequest req) { return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true,"Vehicle service created successfully",service.create(tenantId(req),body,files))); }
  @PostMapping(value = "/{id}/invoices", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasAuthority('CAN_UPDATE_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<List<InvoiceResponse>>> uploadInvoices(@PathVariable UUID id,
      @RequestPart("invoices") MultipartFile[] files,HttpServletRequest req) { return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true,"Vehicle service invoices uploaded successfully",service.uploadInvoices(tenantId(req),id,files))); }
  @PostMapping(value = "/{id}/invoices", consumes = MediaType.APPLICATION_JSON_VALUE) @PreAuthorize("hasAuthority('CAN_UPDATE_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<List<InvoiceResponse>>> uploadInvoicesBase64(@PathVariable UUID id,
      @Valid @RequestBody InvoiceUploadRequest body,HttpServletRequest req) { return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true,"Vehicle service invoices uploaded successfully",service.uploadInvoicesBase64(tenantId(req),id,body.invoices()))); }
  @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE) @PreAuthorize("hasAuthority('CAN_UPDATE_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<VehicleServiceRecord>> update(@PathVariable UUID id,@Valid @RequestBody VehicleServiceRequest body,HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"Vehicle service updated successfully",service.update(tenantId(req),id,body))); }
  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasAuthority('CAN_UPDATE_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<VehicleServiceRecord>> updateWithInvoices(@PathVariable UUID id,
      @RequestPart("service") @Valid VehicleServiceRequest body,
      @RequestPart(value = "invoices", required = false) MultipartFile[] files,
      HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"Vehicle service updated successfully",service.update(tenantId(req),id,body,files))); }
  @PutMapping("/{id}/status/{status}") @PreAuthorize("hasAuthority('CAN_UPDATE_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<VehicleServiceRecord>> updateStatus(@PathVariable UUID id,@PathVariable VehicleServiceRecord.ServiceStatus status,HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"Vehicle service status updated successfully",service.updateStatus(tenantId(req),id,status))); }
  @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('CAN_DELETE_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id,HttpServletRequest req) { service.delete(tenantId(req),id); return ResponseEntity.ok(new ApiResponse<>(true,"Vehicle service deleted successfully",null)); }
  private static UUID tenantId(HttpServletRequest req) { return ((Tenant)req.getAttribute("tenant")).getId(); }
  private static String userName(HttpServletRequest req) {
    UserAccount user = (UserAccount) req.getAttribute("user");
    if (user == null) throw new IllegalArgumentException("Authenticated user not found");
    if (user.getUsername() != null && !user.getUsername().isBlank()) return user.getUsername().trim();
    if (user.getEmail() != null && !user.getEmail().isBlank()) return user.getEmail().trim();
    return user.getId().toString();
  }
  private static void validateDateRange(Long fromDate,Long toDate) { if(fromDate!=null && toDate!=null && toDate<fromDate) throw new IllegalArgumentException("toDate must be greater than or equal to fromDate"); }
}
