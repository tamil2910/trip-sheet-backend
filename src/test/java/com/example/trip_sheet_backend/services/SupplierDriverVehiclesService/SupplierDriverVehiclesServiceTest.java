package com.example.trip_sheet_backend.services.SupplierDriverVehiclesService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;

import com.example.trip_sheet_backend.common.services.UniqueCodeGeneratorService;
import com.example.trip_sheet_backend.dtos.SupplierDriverVehiclesDtos.SupplierDriverVehiclesCreateRequestDTO;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.DriverTenantMapping;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.RoleGroup;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.models.SupplierDriverVehicles;
import com.example.trip_sheet_backend.repositories.DriverRepository;
import com.example.trip_sheet_backend.repositories.DriverTenantMappingRepository;
import com.example.trip_sheet_backend.repositories.RoleGroupRepository;
import com.example.trip_sheet_backend.repositories.RoleRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.repositories.VehicleTypeRepository;
import com.example.trip_sheet_backend.repositories.SupplierDriverVehiclesRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRepository;
import com.example.trip_sheet_backend.services.SupplierDriverVehiclesService.SupplierDriverVehiclesService;

import jakarta.validation.Validation;

class SupplierDriverVehiclesServiceTest {

  @Test
  void rejectsMalformedDriverEmailAndPhone() {
    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      var request = driverRequest();
      request.setPhone("1234567890");
      request.setEmail("not-an-email");

      var violations = validatorFactory.getValidator().validate(request);

      assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("phone")));
      assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("email")));
    }
  }

  @Test
  void rejectsDuplicateDriverPhoneOrEmailForPartner() {
    TestContext context = context();
    SupplierDriverVehiclesCreateRequestDTO request = driverRequest();
    request.setPartnerVendorId(context.partnerVendor.getId());
    when(context.repository.existsByPrimaryVendor_IdAndPartnerVendor_IdAndPhone(
        context.primaryVendor.getId(), context.partnerVendor.getId(), request.getPhone())).thenReturn(true);

    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> context.service.create(request, context.primaryVendor, null));

    assertEquals("A driver with this phone or email already exists for this partner", exception.getMessage());
    verifyNoInteractions(context.userAccountRepository, context.driverRepository);
  }

  @Test
  void rejectsDuplicateDriverEmailForPartner() {
    TestContext context = context();
    SupplierDriverVehiclesCreateRequestDTO request = driverRequest();
    request.setPartnerVendorId(context.partnerVendor.getId());
    when(context.repository.existsByPrimaryVendor_IdAndPartnerVendor_IdAndEmailIgnoreCase(
        context.primaryVendor.getId(), context.partnerVendor.getId(), request.getEmail())).thenReturn(true);

    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> context.service.create(request, context.primaryVendor, null));

    assertEquals("A driver with this phone or email already exists for this partner", exception.getMessage());
    verify(context.repository).existsByPrimaryVendor_IdAndPartnerVendor_IdAndEmailIgnoreCase(
        context.primaryVendor.getId(), context.partnerVendor.getId(), request.getEmail());
    verifyNoInteractions(context.userAccountRepository, context.driverRepository);
  }

  @Test
  void rejectsDuplicateVehicleNumberForPartner() {
    TestContext context = context();
    SupplierDriverVehiclesCreateRequestDTO request = new SupplierDriverVehiclesCreateRequestDTO();
    request.setPartnerVendorId(context.partnerVendor.getId());
    request.setType(SupplierDriverVehicles.Type.VEHICLE);
    request.setVehicleNumber("ka01ab1234");
    request.setVehicleTypeId(UUID.randomUUID());
    when(context.repository.existsByPrimaryVendor_IdAndPartnerVendor_IdAndVehicleNumberIgnoreCase(
        context.primaryVendor.getId(), context.partnerVendor.getId(), "KA01AB1234")).thenReturn(true);

    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> context.service.create(request, context.primaryVendor, null));

    assertEquals("A vehicle with this number already exists for this partner", exception.getMessage());
    verify(context.repository).existsByPrimaryVendor_IdAndPartnerVendor_IdAndVehicleNumberIgnoreCase(
        context.primaryVendor.getId(), context.partnerVendor.getId(), "KA01AB1234");
  }

  @Test
  void createsAccountAndDriverWhenNoMatchingAccountExists() {
    TestContext context = context();
    SupplierDriverVehiclesCreateRequestDTO request = driverRequest();
    request.setPartnerVendorId(context.partnerVendor.getId());
    Role driverRole = new Role();
    driverRole.setName("DRIVER");
    RoleGroup driverGroup = new RoleGroup();

    when(context.userAccountRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
    when(context.userAccountRepository.findByPhone(request.getPhone())).thenReturn(Optional.empty());
    when(context.roleRepository.findByName("DRIVER")).thenReturn(Optional.of(driverRole));
    when(context.roleGroupRepository.findByNameAndTenantIsNull("DRIVER_GLOBAL_PERMISSIONS"))
        .thenReturn(Optional.of(driverGroup));
    when(context.uniqueCodeGeneratorService.generateUniqueCode(anyString(), any())).thenReturn("DRV-ABC123");
    when(context.userAccountRepository.findByUsername(anyString())).thenReturn(Optional.empty());
    when(context.driverRepository.existsByDriverCode("DRV-ABC123")).thenReturn(false);
    when(context.driverRepository.findByAccount_Id(any(UUID.class))).thenReturn(Optional.empty());
    when(context.driverTenantMappingRepository.findByDriver_IdAndTenant_Id(any(UUID.class), any(UUID.class)))
        .thenReturn(Optional.empty());
    when(context.userAccountRepository.save(any(UserAccount.class))).thenAnswer(saveWithId());
    when(context.driverRepository.save(any(Driver.class))).thenAnswer(saveWithId());
    when(context.driverTenantMappingRepository.save(any(DriverTenantMapping.class))).thenAnswer(saveWithId());
    when(context.repository.save(any(SupplierDriverVehicles.class))).thenAnswer(saveWithId());

    SupplierDriverVehicles created = context.service.create(request, context.primaryVendor, "creator-id");

    assertEquals(SupplierDriverVehicles.Type.DRIVER, created.getType());
    assertEquals("Arun Kumar", created.getDriverName());
    assertEquals("arun@example.com", created.getDriver().getAccount().getEmail());
    assertEquals("9876543210", created.getDriver().getAccount().getPhone());
    assertEquals("DRV-ABC123", created.getDriver().getDriverCode());
    verify(context.driverTenantMappingRepository).save(any(DriverTenantMapping.class));
  }

  private <T> Answer<T> saveWithId() {
    return invocation -> {
      T entity = invocation.getArgument(0);
      if (entity instanceof com.example.trip_sheet_backend.common.models.BaseModel baseModel
          && baseModel.getId() == null) {
        baseModel.setId(UUID.randomUUID());
      }
      return entity;
    };
  }

  private TestContext context() {
    SupplierDriverVehiclesRepository repository = mock(SupplierDriverVehiclesRepository.class);
    VendorPartnerRepository vendorPartnerRepository = mock(VendorPartnerRepository.class);
    UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
    TenantRepository tenantRepository = mock(TenantRepository.class);
    DriverRepository driverRepository = mock(DriverRepository.class);
    DriverTenantMappingRepository driverTenantMappingRepository = mock(DriverTenantMappingRepository.class);
    RoleRepository roleRepository = mock(RoleRepository.class);
    RoleGroupRepository roleGroupRepository = mock(RoleGroupRepository.class);
    VehicleTypeRepository vehicleTypeRepository = mock(VehicleTypeRepository.class);
    UniqueCodeGeneratorService uniqueCodeGeneratorService = mock(UniqueCodeGeneratorService.class);

    Tenant primaryVendor = new Tenant();
    primaryVendor.setId(UUID.randomUUID());
    primaryVendor.setTenantType(Tenant.TenantType.VENDOR);

    Tenant partnerVendor = new Tenant();
    partnerVendor.setId(UUID.randomUUID());
    partnerVendor.setTenantType(Tenant.TenantType.VENDOR);

    VendorPartner partnership = new VendorPartner();
    partnership.setPrimaryVendor(primaryVendor);
    partnership.setPartnerVendor(partnerVendor);

    when(tenantRepository.findById(partnerVendor.getId())).thenReturn(Optional.of(partnerVendor));
    when(vendorPartnerRepository.findByPrimaryVendorAndPartnerVendor(primaryVendor, partnerVendor))
        .thenReturn(Optional.of(partnership));

    SupplierDriverVehiclesService service = new SupplierDriverVehiclesService(
        repository,
        vendorPartnerRepository,
        userAccountRepository,
        tenantRepository,
        driverRepository,
      driverTenantMappingRepository,
      roleRepository,
      roleGroupRepository,
      vehicleTypeRepository,
      uniqueCodeGeneratorService);

    return new TestContext(service, repository, userAccountRepository, driverRepository,
      driverTenantMappingRepository, roleRepository, roleGroupRepository, uniqueCodeGeneratorService,
      primaryVendor, partnerVendor);
  }

  private SupplierDriverVehiclesCreateRequestDTO driverRequest() {
    SupplierDriverVehiclesCreateRequestDTO request = new SupplierDriverVehiclesCreateRequestDTO();
    request.setType(SupplierDriverVehicles.Type.DRIVER);
    request.setDriverName("Arun Kumar");
    request.setPhone("9876543210");
    request.setEmail("arun@example.com");
    return request;
  }

  private record TestContext(
      SupplierDriverVehiclesService service,
      SupplierDriverVehiclesRepository repository,
      UserAccountRepository userAccountRepository,
      DriverRepository driverRepository,
      DriverTenantMappingRepository driverTenantMappingRepository,
      RoleRepository roleRepository,
      RoleGroupRepository roleGroupRepository,
      UniqueCodeGeneratorService uniqueCodeGeneratorService,
      Tenant primaryVendor,
      Tenant partnerVendor
  ) {}
}