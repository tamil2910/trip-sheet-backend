package com.example.trip_sheet_backend.services.DutyTypeService;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.repositories.DutyTypeRepository;

@Service
public class DutyTypeServiceImp extends BaseServiceImp<DutyType, UUID> implements DutyTypeService {

  public DutyTypeServiceImp(DutyTypeRepository repository) {
    super(repository);
  }
}
