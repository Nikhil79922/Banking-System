package com.banking.transaction_service.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class Pagination {

    public Pageable getPageable(int skip, int limit , String sort , String properties) {
        Sort.Direction direction =
                "asc".equalsIgnoreCase(sort)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        return PageRequest.of(
                skip / limit,
                limit,
                Sort.by(direction, properties)
        );
    }
}
