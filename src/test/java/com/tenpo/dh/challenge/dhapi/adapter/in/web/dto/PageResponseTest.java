package com.tenpo.dh.challenge.dhapi.adapter.in.web.dto;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void from_mapsAllPageFieldsCorrectly() {
        List<String> items = List.of("a", "b");
        Page<String> page = new PageImpl<>(items, PageRequest.of(2, 5), 20);

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.totalElements()).isEqualTo(20);
        assertThat(response.totalPages()).isEqualTo(4);
    }

    @Test
    void from_emptyPage_returnsEmptyContentWithZeroTotals() {
        Page<String> page = new PageImpl<>(List.of());

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.totalPages()).isEqualTo(1);
    }
}
