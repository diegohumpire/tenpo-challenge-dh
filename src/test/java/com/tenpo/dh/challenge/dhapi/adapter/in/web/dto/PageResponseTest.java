package com.tenpo.dh.challenge.dhapi.adapter.in.web.dto;

import com.tenpo.dh.challenge.dhapi.domain.model.PaginationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void from_mapsAllPageFieldsCorrectly() {
        List<String> items = List.of("a", "b");
        PaginationResult<String> result = new PaginationResult<>(items, 2, 5, 20L, 4);

        PageResponse<String> response = PageResponse.from(result);

        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.totalElements()).isEqualTo(20);
        assertThat(response.totalPages()).isEqualTo(4);
    }

    @Test
    void from_emptyResult_returnsEmptyContentWithZeroTotals() {
        PaginationResult<String> result = new PaginationResult<>(List.of(), 0, 20, 0L, 0);

        PageResponse<String> response = PageResponse.from(result);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.totalPages()).isEqualTo(0);
    }
}
