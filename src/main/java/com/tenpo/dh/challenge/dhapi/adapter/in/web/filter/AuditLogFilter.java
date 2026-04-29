package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.dh.challenge.dhapi.domain.port.out.AuditEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class AuditLogFilter implements WebFilter {

    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "/actuator", "/swagger-ui", "/v3/api-docs", "/mock", "/webjars", "/mock/percentage");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AuditEventPublisher auditEventPublisher;
    private final WebExchangeAuditLogMapper auditLogMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (isExcluded(path)) {
            log.info("Excluded path `{}`", path);
            return chain.filter(exchange);
        }

        long startTime = System.currentTimeMillis();
        // X-Transactional-Id and X-User-Id are guaranteed to be present by
        // RequestHeadersFilter, which runs at @Order(2) before this filter.
        String transactionalId = exchange.getRequest().getHeaders()
                .getFirst(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID);
        String userId = exchange.getRequest().getHeaders()
                .getFirst(RequestHeadersFilter.HEADER_USER_ID);

        // Eagerly read and buffer the entire request body once.
        // This prevents multiple subscriptions to the underlying (potentially unicast)
        // body publisher that would occur if the decorator's getBody() were subscribed
        // to more than once by the handler or the test infrastructure.
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .map(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    return bytes;
                })
                .defaultIfEmpty(new byte[0])
                .flatMap(bytes -> {
                    String requestBody = bytes.length > 0
                            ? sanitizeRequestBody(new String(bytes, StandardCharsets.UTF_8))
                            : "";

                    // Provide a fresh DataBuffer view of the pre-read bytes on every
                    // getBody() call so the handler can always deserialize the body.
                    ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return Mono.fromSupplier(() -> exchange.getResponse().bufferFactory().wrap(bytes)).flux();
                        }
                    };

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(decoratedRequest)
                            .build();

                    return chain.filter(mutatedExchange)
                            .doFinally(signal -> {
                                try {
                                    ServerHttpRequest req = exchange.getRequest();
                                    ServerHttpResponse resp = mutatedExchange.getResponse();

                                    WebExchangeAuditContext context = new WebExchangeAuditContext(
                                            transactionalId,
                                            userId,
                                            path,
                                            req.getMethod().name(),
                                            formatQueryParams(req),
                                            headersToString(req.getHeaders()),
                                            requestBody,
                                            resp.getStatusCode() != null ? resp.getStatusCode().value() : null,
                                            System.currentTimeMillis() - startTime);

                                    auditEventPublisher.publish(auditLogMapper.toAuditLog(context));
                                } catch (Exception e) {
                                    log.error("Error building audit log entry: {}", e.getMessage());
                                }
                            });
                });
    }

    private boolean isExcluded(String path) {
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * Compacts a request body string to a single-line JSON with no extra whitespace.
     * If the body is not valid JSON, returns a single-line trimmed version.
     */
    private String sanitizeRequestBody(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }
        try {
            Object parsed = OBJECT_MAPPER.readValue(body, Object.class);
            return OBJECT_MAPPER.writeValueAsString(parsed);
        } catch (Exception e) {
            return body.replaceAll("\\s+", " ").strip();
        }
    }

    private String formatQueryParams(ServerHttpRequest req) {
        var params = req.getQueryParams();
        if (params.isEmpty())
            return null;
        return params.toSingleValueMap().entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    private String headersToString(HttpHeaders headers) {
        return headers.toSingleValueMap().toString();
    }
}
