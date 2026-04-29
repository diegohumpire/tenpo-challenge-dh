package com.tenpo.dh.challenge.dhapi.domain.model;

public record PaginationRequest(int page, int size, String sortField, SortDirection sortDirection) {

    public enum SortDirection {
        ASC, DESC
    }
}
