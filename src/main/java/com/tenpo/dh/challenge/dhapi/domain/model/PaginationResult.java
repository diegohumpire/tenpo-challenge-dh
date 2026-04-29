package com.tenpo.dh.challenge.dhapi.domain.model;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public record PaginationResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public <R> PaginationResult<R> map(Function<T, R> mapper) {
        return new PaginationResult<>(
                content.stream().map(mapper).collect(Collectors.toList()),
                page, size, totalElements, totalPages
        );
    }
}
