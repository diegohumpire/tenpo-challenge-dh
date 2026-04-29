package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that gateway-injected traceability headers are present on every
 * non-excluded request.
 *
 * <p>This service is expected to run behind an API Gateway (e.g. Apigee,
 * Azure API Management) that authenticates the caller and injects:
 * <ul>
 *   <li>{@code X-Transactional-Id} — correlation ID for distributed tracing.</li>
 *   <li>{@code X-User-Id} — authenticated user identity.</li>
 * </ul>
 *
 * <p>If either header is absent or blank a {@code 400 Bad Request} response is
 * returned in {@code application/problem+json} format before the request
 * reaches any business logic.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class RequestHeadersFilter implements WebFilter {

    public static final String HEADER_TRANSACTIONAL_ID = "X-Transactional-Id";
    public static final String HEADER_USER_ID = "X-User-Id";

    private final FilterResponseWriter filterResponseWriter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (FilterExclusionConfig.isExcluded(path)) {
            return chain.filter(exchange);
        }

        List<String> missing = resolveMissingHeaders(exchange);
        if (!missing.isEmpty()) {
            log.warn("Missing required headers {} for path {}", missing, path);
            String detail = "Required headers are missing: " + missing;
            return filterResponseWriter.writeBadRequest(exchange, "Missing Required Headers", detail);
        }

        return chain.filter(exchange);
    }

    private List<String> resolveMissingHeaders(ServerWebExchange exchange) {
        List<String> missing = new ArrayList<>();
        if (isBlank(exchange.getRequest().getHeaders().getFirst(HEADER_TRANSACTIONAL_ID))) {
            missing.add(HEADER_TRANSACTIONAL_ID);
        }
        if (isBlank(exchange.getRequest().getHeaders().getFirst(HEADER_USER_ID))) {
            missing.add(HEADER_USER_ID);
        }
        return missing;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
