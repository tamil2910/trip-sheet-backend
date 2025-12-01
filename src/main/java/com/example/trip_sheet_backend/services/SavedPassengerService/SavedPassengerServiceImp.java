package com.example.trip_sheet_backend.services.SavedPassengerService;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.models.SavedPassenger;
import com.example.trip_sheet_backend.repositories.SavedPassengerRepository;

@Service
public class SavedPassengerServiceImp extends BaseServiceImp<SavedPassenger, UUID> implements SavedPassengerService {
  public SavedPassengerServiceImp(SavedPassengerRepository repository) {
    super(repository);
  }
}
