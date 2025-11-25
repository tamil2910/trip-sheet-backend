package com.example.trip_sheet_backend.controllers.tenants;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.dtos.VehicleTypeCreateRequestDto;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.models.VehicleType.typeVehicle;
import com.example.trip_sheet_backend.models.VehicleTypeCustomName;
import com.example.trip_sheet_backend.repositories.VehicleTypeCustomNamesRepository;
import com.example.trip_sheet_backend.repositories.VehicleTypeRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.TenantService.TenantService;
import com.example.trip_sheet_backend.services.VehicleTypeCustomNamesService.VehicleTypeCustomNamesService;
import com.example.trip_sheet_backend.services.VehicleTypeService.VehicleTypeService;

@RestController
@RequestMapping("/vehicletype-custom-name")
public class VehicleTypeCustomNameController extends BaseController<VehicleTypeCustomName, UUID> {

  private final VehicleTypeCustomNamesRepository repository;
  private final VehicleTypeRepository vehicleTypeRepository;


  private final VehicleTypeCustomNamesService service;
  private final VehicleTypeService vehicleTypeService;
  private final TenantService tenantService;

  public VehicleTypeCustomNameController(VehicleTypeCustomNamesRepository repository, VehicleTypeCustomNamesService service, 
    VehicleTypeRepository vehicleTypeRepository, VehicleTypeService vehicleTypeService, TenantService tenantService) {
    super(service);
    this.repository = repository;
    this.vehicleTypeRepository = vehicleTypeRepository;
    this.service = service;
    this.vehicleTypeService = vehicleTypeService;
    this.tenantService = tenantService;
  }

  @PreAuthorize("hasAnyRole('SUPER_ADMIN, 'ADMIN')")
  @PostMapping("/add-custom-type")
  public ResponseEntity<ApiResponse<VehicleTypeCustomName>> create_vehicle_type(@RequestBody VehicleTypeCreateRequestDto body) {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String tenantId = (String) auth.getDetails();

    if (body.getTypeOfVehicle() == null || body.getSeatCount() == null) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Type of vehicle and seat count are required to add vehicle type", null));
    }

    Optional<VehicleType> exist = this.vehicleTypeRepository.findBySeatCountAndTypeOfVehicle(body.getSeatCount(), body.getTypeOfVehicle());

    VehicleTypeCustomName customPayload = new VehicleTypeCustomName();
    typeVehicle type_of_vehicle = body.getTypeOfVehicle();


    switch (type_of_vehicle) {
      case SEDAN, HATCHBACK -> {
        if (body.getSeatCount() < 4 && body.getSeatCount() > 5) {
          return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Seat count either can be 4 or 5 for sedan (or) hatchback vehicle type", null));
        }
      }
      case SUV -> {
        if (body.getSeatCount() < 5 && body.getSeatCount() > 7) {
          return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Seat count either can be 5 or 7 for sedan (or) hatchback vehicle type", null));
        }
      }
      case MUV -> {
        if (body.getSeatCount() < 5 && body.getSeatCount() > 9) {
          return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Seat count either can be 7 or 9 for sedan (or) hatchback vehicle type", null));
        }
      }
      default -> {}
    }

    VehicleType vt = null;

    // new entry
    if(exist.isPresent()) {
      vt = exist.get();
    }
    else { // exist one
      vt = new VehicleType();
  
      vt.setTypeOfVehicle(type_of_vehicle);
      vt.setSeatCount(body.getSeatCount());
      vt.setDefaultName(vt.getTypeOfVehicle() + "_" + vt.getSeatCount());
      vt.setDescription(body.getDescription());
      vt.setIsGlobal(false);

      vt = this.vehicleTypeService.createResource(vt);
    }

    // 3. Now handle customName table

    Tenant tenant = this.tenantService.findByIdResource(UUID.fromString(tenantId));
    Optional<VehicleTypeCustomName> customExist = this.repository.findByVehicleTypeAndTenant(vt.getId(), tenant.getId());

    if (customExist.isPresent()) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiResponse<>(false, "Custom name already exists for this tenant", null));
    }


    VehicleTypeCustomName custom = new VehicleTypeCustomName();
    custom.setTenant(tenant);
    custom.setVehicleType(vt);
    custom.setCustomName(body.getCustomName()); // from body

    repository.save(custom);

    return ResponseEntity.ok().body(new ApiResponse<>(true, "Vehicle Type has saved successfully!", custom));

  }



}
