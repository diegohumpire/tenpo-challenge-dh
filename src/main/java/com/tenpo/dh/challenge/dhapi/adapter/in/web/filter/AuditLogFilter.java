package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.CallDirection;
import com.tenpo.dh.challenge.dhapi.domain.port.in.AuditLogUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class AuditLogFilter implements WebFilter {

    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "/actuator", "/swagger-ui", "/v3/api-docs", "/mock", "/webjars"
    );

    private final AuditLogUseCase auditLogUseCase;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (isExcluded(path)) {
            return chain.filter(exchange);
        }

        long startTime = System.currentTimeMillis();
        String transactionalId = UUID.randomUUID().toString();
        AtomicReference<String> requestBodyRef = new AtomicReference<>("");
        AtomicReference<String> responseBodyRef = new AtomicReference<>("");

        ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public Flux<DataBuffer> getBody() {
                return DataBufferUtils.join(super.getBody())
                        .map(buffer -> {
                            byte[] bytes = new byte[buffer.readableByteCount()];
                            buffer.read(bytes);
                            DataBufferUtils.release(buffer);
                            requestBodyRef.set(new String(bytes, StandardCharsets.UTF_8));
                            return exchange.getResponse().bufferFactory().wrap(bytes);
                        })
                        .flux();
            }
        };

        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
        ServerHttpResponse decoratedResponse = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                Flux<DataBuffer> flux = Flux.from(body)
                        .map(buffer -> {
                            byte[] bytes = new byte[buffer.readableByteCount()];
                            buffer.read(bytes);
                            DataBufferUtils.release(buffer);
                            responseBodyRef.set(new String(bytes, StandardCharsets.UTF_8));
                            return bufferFactory.wrap(bytes);
                        });
                return super.writeWith(flux);
            }
        };

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(decoratedRequest)
                .response(decoratedResponse)
                .build();

        return chain.filter(mutatedExchange)
                .doFinally(signal -> {
                    try {
                        long durationMs = System.currentTimeMillis() - startTime;
                        ServerHttpRequest req = mutatedExchange.getRequest();
                        ServerHttpResponse resp = mutatedExchange.getResponse();

                        AuditLog auditLog = AuditLog.builder()
                                .createdAt(OffsetDateTime.now())
                                .action(resolveAction(req))
                                .actionType(AuditActionType.HTTP_REQUEST)
                                .callDirection(CallDirection.IN)
                                .transactionalId(transactionalId)
                                .method(req.getMethod().name())
                                .endpoint(path)
                                .params(formatQueryParams(req))
                                .requestHeaders(headersToString(req.getHeaders()))
                                .requestBody(requestBodyRef.get())
                                .responseHeaders(resp.getHeaders() != null ? headersToString(resp.getHeaders()) : null)
                                .responseBody(responseBodyRef.get())
                                .statusCode(resp.getStatusCode() != null ? resp.getStatusCode().value() : null)
                                .durationMs(durationMs)
                                .build();

                        auditLogUseCase.save(auditLog)
                                .subscribe(
                                        saved -> {},
                                        err -> log.error("Async audit log save failed: {}", err.getMessage())
                                );
                    } catch (Exception e) {
                        log.error("Error building audit log entry: {}", e.getMessage());
                    }
                });
    }

    private boolean isExcluded(String path) {
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private String resolveAction(ServerHttpRequest req) {
        String path = req.getPath().value();
        String method = req.getMethod().name();
        if (path.contains("/calculations")) return "CREATE_CALCULATION";
        if (path.contains("/audit-logs")) return "GET_AUDIT_LOGS";
        return method + "_" + path.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();
    }

    private String formatQueryParams(ServerHttpRequest req) {
        var params = req.getQueryParams();
        if (params.isEmpty()) return null;
        return params.toSingleValueMap().entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    private String headersToString(HttpHeaders headers) {
        return headers.toSingleValueMap().toString();
    }
}
