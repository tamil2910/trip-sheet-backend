package com.example.trip_sheet_backend.common.services;

import java.io.Serializable;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface GlobalBaseService<T, ID extends Serializable> {
    T create(T payload);
    T findById(ID id);
    Page<T> getAll(Pageable pageable);
    T update(ID id, T payload);
    void delete(ID id);
    Page<T> search(Map<String, Object> filters, Pageable pageable);

}
