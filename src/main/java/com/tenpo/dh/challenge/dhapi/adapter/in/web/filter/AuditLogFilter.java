package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import com.tenpo.dh.challenge.dhapi.domain.port.out.AuditEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class AuditLogFilter implements WebFilter {

    private final AuditEventPublisher auditEventPublisher;
    private final WebExchangeAuditLogMapper auditLogMapper;
    private final BodyCompactor bodyCompactor;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (FilterExcludedPaths.isExcluded(path)) {
            log.debug("Path excluido del audit log: `{}`", path);
            return chain.filter(exchange);
        }

        long startTime = System.currentTimeMillis();
        // X-Transactional-Id y X-User-Id están asegurados por RequestHeadersFilter (@Order(2)).
        String transactionalId = exchange.getRequest().getHeaders()
                .getFirst(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID);
        String userId = exchange.getRequest().getHeaders()
                .getFirst(RequestHeadersFilter.HEADER_USER_ID);

        // Lee y almacena en buffer el request body completo una sola vez para permitir
        // múltiples lecturas (el body Publisher subyacente es unicast).
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
                            ? bodyCompactor.compact(new String(bytes, StandardCharsets.UTF_8))
                            : "";

                    RequestBodyBufferingDecorator decoratedRequest =
                            new RequestBodyBufferingDecorator(exchange.getRequest(), bytes,
                                    exchange.getResponse().bufferFactory());

                    AtomicReference<String> responseBodyRef = new AtomicReference<>("");
                    ResponseCapturingDecorator responseDecorator =
                            new ResponseCapturingDecorator(exchange.getResponse(), responseBodyRef,
                                    transactionalId, bodyCompactor);

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(decoratedRequest)
                            .response(responseDecorator)
                            .build();

                    return chain.filter(mutatedExchange)
                            .doFinally(signal -> publishAuditLog(
                                    exchange, mutatedExchange, path,
                                    requestBody, responseBodyRef,
                                    startTime, transactionalId, userId));
                });
    }

    private void publishAuditLog(ServerWebExchange original, ServerWebExchange mutated,
                                  String path, String requestBody,
                                  AtomicReference<String> responseBodyRef,
                                  long startTime, String transactionalId, String userId) {
        try {
            ServerHttpRequest req = original.getRequest();
            ServerHttpResponse resp = mutated.getResponse();

            WebExchangeAuditContext context = new WebExchangeAuditContext(
                    transactionalId, userId,
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
