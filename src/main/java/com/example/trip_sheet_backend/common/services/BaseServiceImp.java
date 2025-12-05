package com.example.trip_sheet_backend.common.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;

import jakarta.persistence.criteria.Predicate;


public class BaseServiceImp<T, ID extends Serializable> implements BaseService<T, ID> {

  protected final BaseRepository<T, ID> repository;
  // protected final JpaSpecificationExecutor<T> specExecutor;

  public BaseServiceImp(BaseRepository<T, ID> repository) {
    this.repository = repository;
    // this.specExecutor = specExecutor;
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

  @Override
  public Page<T> searchResources(Map<String, Object> filters, Pageable pageable) {

    Specification<T> specification = buildSpecification(filters);
    return repository.findAll(specification, pageable);
  }

  private Specification<T> buildSpecification(Map<String, Object> filters) {

    return (root, query,  cb) -> {

      if (filters == null || filters.isEmpty()) 
        return cb.conjunction();

      List<Predicate> predicates = new ArrayList<>();

      filters.forEach((key, value) -> {
        if (value != null) {
          
          if (value instanceof String strVal) {
            predicates.add(cb.like(cb.lower(root.get(key)), "%" + strVal.toLowerCase()));
          } else {
            predicates.add(cb.equal(root.get(key), value));
          }
        }
      });

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

}
