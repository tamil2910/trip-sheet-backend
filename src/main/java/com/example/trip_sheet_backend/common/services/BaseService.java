package com.example.trip_sheet_backend.common.services;

import java.io.Serializable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BaseService<T, ID extends Serializable> {
  T createResource(T payload);

  T findByIdResource(ID id);

  Page<T> getAllResources(Pageable pageable);

  T updateResource(ID id, T payload);

  void deleteResource(ID id);
}
