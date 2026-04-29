package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class FilterExcludedPathsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/actuator",
            "/actuator/health",
            "/swagger-ui",
            "/swagger-ui/index.html",
            "/v3/api-docs",
            "/v3/api-docs/swagger-config",
            "/mock",
            "/mock/percentage",
            "/webjars/some-lib"
    })
    void isExcluded_returnsTrueForExcludedPrefixes(String path) {
        assertThat(FilterExcludedPaths.isExcluded(path)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/calculations",
            "/api/v1/audit-logs",
            "/",
            "/health",
            "/calculations"
    })
    void isExcluded_returnsFalseForNonExcludedPaths(String path) {
        assertThat(FilterExcludedPaths.isExcluded(path)).isFalse();
    }

    @Test
    void excludedPrefixes_listContainsExpectedValues() {
        assertThat(FilterExcludedPaths.EXCLUDED_PREFIXES)
                .contains("/actuator", "/swagger-ui", "/v3/api-docs", "/mock", "/webjars");
    }
}

