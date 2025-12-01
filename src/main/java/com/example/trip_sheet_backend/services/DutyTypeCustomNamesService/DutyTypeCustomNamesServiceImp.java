package com.example.trip_sheet_backend.services.DutyTypeCustomNamesService;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.models.DutyTypeCustomName;
import com.example.trip_sheet_backend.repositories.DutyTypeCustomNamesRepository;
import com.example.trip_sheet_backend.repositories.DutyTypeRepository;
@Service
public class DutyTypeCustomNamesServiceImp extends BaseServiceImp<DutyTypeCustomName, UUID> implements DutyTypeCustomNamesService {
  DutyTypeRepository dutyTypeRepository;
  public DutyTypeCustomNamesServiceImp(DutyTypeCustomNamesRepository repository, DutyTypeRepository dutyTypeRepository) {
    super(repository);
    this.dutyTypeRepository = dutyTypeRepository;
  }

  
}
