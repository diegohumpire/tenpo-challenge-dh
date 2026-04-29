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
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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
            log.info("Path excluido `{}`", path);
            return chain.filter(exchange);
        }

        long startTime = System.currentTimeMillis();
        // X-Transactional-Id y X-User-Id están asegurados por RequestHeadersFilter,
        // que se ejecuta en @Order(2) antes que este filter.
        String transactionalId = exchange.getRequest().getHeaders()
                .getFirst(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID);
        String userId = exchange.getRequest().getHeaders()
                .getFirst(RequestHeadersFilter.HEADER_USER_ID);

        // Lee y almacena en buffer el request body completo una sola vez.
        // Esto evita múltiples subscripciones al body publisher subyacente
        // (potencialmente unicast), que sucederían si el getBody() del decorator
        // (decorator) fuera suscrito más de una vez por el handler o la infra
        // de tests.
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

                    // Provee una vista nueva del DataBuffer con los bytes ya leídos en cada
                    // llamada a getBody(), para que el handler siempre pueda deserializar el body.
                    ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return Mono.fromSupplier(() -> exchange.getResponse().bufferFactory().wrap(bytes)).flux();
                        }
                    };

                    // Captura el response body e inyecta X-Transactional-Id en los response
                    // headers.
                    AtomicReference<String> responseBodyRef = new AtomicReference<>("");
                    ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(
                            exchange.getResponse()) {
                        @Override
                        public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                            return DataBufferUtils.join(Flux.from(body))
                                    .flatMap(buffer -> {
                                        byte[] respBytes = new byte[buffer.readableByteCount()];
                                        buffer.read(respBytes);
                                        DataBufferUtils.release(buffer);
                                        responseBodyRef.set(sanitizeRequestBody(
                                                new String(respBytes, StandardCharsets.UTF_8)));
                                        return super.writeWith(
                                                Mono.fromSupplier(() -> bufferFactory().wrap(respBytes)));
                                    })
                                    .switchIfEmpty(super.writeWith(Flux.empty()));
                        }
                    };

                    // Inyecta X-Transactional-Id en cada respuesta antes de que los headers sean
                    // confirmados (committed).
                    responseDecorator.beforeCommit(() -> {
                        responseDecorator.getHeaders().set(
                                RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, transactionalId);
                        return Mono.empty();
                    });

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(decoratedRequest)
                            .response(responseDecorator)
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
                                            headersToString(resp.getHeaders()),
                                            responseBodyRef.get(),
                                            resp.getStatusCode() != null ? resp.getStatusCode().value() : null,
                                            System.currentTimeMillis() - startTime);

                                    auditEventPublisher.publish(auditLogMapper.toAuditLog(context));
                                } catch (Exception e) {
                                    log.error("Error al construir el audit log entry: {}", e.getMessage());
                                }
                            });
                });
    }

    private boolean isExcluded(String path) {
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * Compacta el body string a un JSON de una sola línea sin espacios extra.
     * Si el body no es un JSON válido, retorna una versión de una sola línea sin
     * espacios al inicio/fin.
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
