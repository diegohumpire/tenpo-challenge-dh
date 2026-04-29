package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
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
public class RequestHeadersFilter implements WebFilter {

    public static final String HEADER_TRANSACTIONAL_ID = "X-Transactional-Id";
    public static final String HEADER_USER_ID = "X-User-Id";

    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "/actuator", "/swagger-ui", "/v3/api-docs", "/mock", "/webjars", "/mock/percentage");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (isExcluded(path)) {
            return chain.filter(exchange);
        }

        List<String> missing = resolveMissingHeaders(exchange);
        if (!missing.isEmpty()) {
            log.warn("Missing required headers {} for path {}", missing, path);
            return writeMissingHeadersError(exchange, missing);
        }

        return chain.filter(exchange);
    }

    private boolean isExcluded(String path) {
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith);
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

    private Mono<Void> writeMissingHeadersError(ServerWebExchange exchange, List<String> missing) {
        String detail = "Required headers are missing: " + missing;
        String body = String.format(
                "{\"status\":400,\"title\":\"Missing Required Headers\",\"detail\":\"%s\",\"type\":\"%s\"}",
                detail, URI.create("about:blank"));
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
