package com.example.trip_sheet_backend.common.services;

import java.io.Serializable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public class BaseServiceImp<T, ID extends Serializable> implements BaseService<T, ID> {

  protected final JpaRepository<T,ID> repository;

  public BaseServiceImp(JpaRepository<T, ID> repository) {
    this.repository = repository;
  }

  @Override
  public T createResource(T payload) {
    return repository.save(payload);
  }

  @Override
  public T findByIdResource(ID id) {
    return repository.findById(id).orElse(null);
  }

  @Override
  public Page<T> getAllResources(Pageable pageable) {
    return repository.findAll(pageable);
  }

  @Override
  public T updateResource(ID id, T payload) {
    return repository.save(payload);
  }

  @Override
  public void deleteResource(ID id) {
    repository.deleteById(id);
  }

}
