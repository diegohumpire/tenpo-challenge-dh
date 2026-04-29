package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.CallDirection;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class AuditLogFilter implements WebFilter {

    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "/actuator", "/swagger-ui", "/v3/api-docs", "/mock", "/webjars", "/mock/percentage");

    private final AuditEventPublisher auditEventPublisher;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (isExcluded(path)) {
            return chain.filter(exchange);
        }

        long startTime = System.currentTimeMillis();
        // TODO: Agregrar un filtro de validacion de cabeceras obligatorias (e.g.
        // Transactional-Id) para asegurar su presencia y correcta propagación a través
        // de logs y trazas distribuidas. Mientras tanto
        String transactionalId = UUID.randomUUID().toString(); // TODO: Debe de ser inyectado mediante una cabecera... o
                                                               // generado por un componente específico para ese fin
                                                               // (e.g. TransactionalIdGenerator) para asegurar su
                                                               // propagación a través de logs y trazas distribuidas.

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
                            ? new String(bytes, StandardCharsets.UTF_8)
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
                                    long durationMs = System.currentTimeMillis() - startTime;
                                    ServerHttpRequest req = exchange.getRequest();
                                    ServerHttpResponse resp = mutatedExchange.getResponse();

                                    AuditLog auditLog = AuditLog.builder()
                                            .createdAt(OffsetDateTime.now())
                                            .action(resolveAction(req))
                                            .actionType(resolveActionType(path))
                                            .callDirection(CallDirection.IN)
                                            .transactionalId(transactionalId)
                                            .method(req.getMethod().name())
                                            .endpoint(path)
                                            .params(formatQueryParams(req))
                                            .requestHeaders(headersToString(req.getHeaders()))
                                            .requestBody(requestBody)
                                            .statusCode(
                                                    resp.getStatusCode() != null ? resp.getStatusCode().value() : null)
                                            .durationMs(durationMs)
                                            .build();

                                    auditEventPublisher.publish(auditLog);
                                } catch (Exception e) {
                                    log.error("Error building audit log entry: {}", e.getMessage());
                                }
                            });
                });
    }

    private boolean isExcluded(String path) {
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private String resolveAction(ServerHttpRequest req) {
        String path = req.getPath().value();
        String method = req.getMethod().name();
        if (path.contains("/calculations"))
            return "CREATE_CALCULATION";
        if (path.contains("/audit-logs"))
            return "GET_AUDIT_LOGS";
        return method + "_" + path.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();
    }

    private AuditActionType resolveActionType(String path) {
        if (path.contains("/calculations"))
            return AuditActionType.CALCULATION;
        return AuditActionType.HTTP_REQUEST;
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
