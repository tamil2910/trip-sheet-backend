package com.example.trip_sheet_backend.common.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class GlobalBaseServiceImp<T, ID extends Serializable> implements GlobalBaseService<T, ID> {

    protected final BaseRepository<T, ID> repository;

    public GlobalBaseServiceImp(BaseRepository<T, ID> repository) {
        this.repository = repository;
    }

    @Override
    public T create(T payload) {
        return repository.save(payload);
    }

    @Override
    public T findById(ID id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public Page<T> getAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public T update(ID id, T payload) {
        return repository.save(payload);
    }

    @Override
    public void delete(ID id) {
        repository.deleteById(id);
    }

    @Override
    public Page<T> search(Map<String, Object> filters, Pageable pageable) {
        Specification<T> spec = buildSpecification(filters);
        return repository.findAll(spec, pageable);
    }

private static final Set<String> RESERVED_PARAMS =
        Set.of("page", "size", "sort");

private Specification<T> buildSpecification(Map<String, Object> filters) {

    return (root, query, cb) -> {

        query.distinct(true);

        if (filters == null || filters.isEmpty()) {
            return cb.conjunction();
        }

        List<Predicate> predicates = new ArrayList<>();

        filters.forEach((key, value) -> {

            // 1️⃣ skip pagination & reserved params
            if (value == null || RESERVED_PARAMS.contains(key)) {
                return;
            }

            // 2️⃣ skip unknown fields safely
            if (!hasField(root, key)) {
                return;
            }

            try {
                Path<?> path = root.get(key);
                Class<?> fieldType = path.getJavaType();
                String stringValue = value.toString().trim();

                /* ---------- STRING ---------- */
                if (fieldType.equals(String.class)) {

                    // exact match fields
                    if (key.equalsIgnoreCase("email")
                        || key.equalsIgnoreCase("phone")) {

                        predicates.add(cb.equal(path, stringValue));
                    }
                    // LIKE search fields
                    else {
                        predicates.add(
                            cb.like(
                                cb.lower(path.as(String.class)),
                                "%" + stringValue.toLowerCase() + "%"
                            )
                        );
                    }
                }

                /* ---------- UUID ---------- */
                else if (fieldType.equals(UUID.class)) {
                    predicates.add(
                        cb.equal(path, UUID.fromString(stringValue))
                    );
                }

                /* ---------- ENUM ---------- */
                else if (fieldType.isEnum()) {
                    Object enumValue = Arrays.stream(fieldType.getEnumConstants())
                            .filter(e -> e.toString().equalsIgnoreCase(stringValue))
                            .findFirst()
                            .orElse(null);

                    if (enumValue != null) {
                        predicates.add(cb.equal(path, enumValue));
                    }
                }

                /* ---------- BOOLEAN / NUMBER ---------- */
                else {
                    Object convertedValue = convertValue(fieldType, stringValue);
                    predicates.add(cb.equal(path, convertedValue));
                }

            } catch (Exception ignored) {
                // never break search because of one bad param
            }
        });

        return cb.and(predicates.toArray(new Predicate[0]));
    };

}

    private boolean hasField(Root<T> root, String field) {
        try {
            root.get(field);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    private Object convertValue(Class<?> fieldType, String value) {

        if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
            return Integer.parseInt(value);
        }
        if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
            return Long.parseLong(value);
        }
        if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
            return Boolean.parseBoolean(value);
        }
        if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
            return Double.parseDouble(value);
        }

        return value;
    }






}
