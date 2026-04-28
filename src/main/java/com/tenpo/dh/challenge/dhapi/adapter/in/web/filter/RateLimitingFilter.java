package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitingFilter implements WebFilter {

    // Excluir rutas de documentación y mock para evitar interferencias en pruebas y
    // desarrollo
    // En teoria, no deberian de llegar a produccion. Excepto el `actuator`.
    private static final List<String> EXCLUDED_PREFIXES = List.of("/actuator", "/swagger-ui", "/v3/api-docs", "/mock",
            "/webjars", "/mock/percentage");

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${rate-limit.max-requests:3}")
    private int maxRequests;

    @Value("${rate-limit.window-seconds:60}")
    private long windowSeconds;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (isExcluded(path)) {
            return chain.filter(exchange);
        }

        String clientIp = resolveClientIp(exchange);
        String key = "rate_limit:" + clientIp;

        return redisTemplate.opsForValue().increment(key).flatMap(count -> {
            if (count == 1) {
                return redisTemplate.expire(key, Duration.ofSeconds(windowSeconds)).thenReturn(count);
            }
            return Mono.just(count);
        }).flatMap(count -> {
            long remaining = Math.max(0, maxRequests - count);

            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(maxRequests));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(remaining));

            if (count > maxRequests) {
                log.warn("Rate limit exceeded for IP={}, count={}", clientIp, count);
                return redisTemplate.getExpire(key).flatMap(ttl -> {
                    long retryAfter = ttl.getSeconds() > 0 ? ttl.getSeconds() : windowSeconds;
                    exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(retryAfter));
                    exchange.getResponse().getHeaders().add("X-RateLimit-Reset",
                            String.valueOf(System.currentTimeMillis() / 1000 + retryAfter));
                    return writeRateLimitResponse(exchange, retryAfter);
                });
            }

            return chain.filter(exchange);
        });
    }

    private boolean isExcluded(String path) {
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> writeRateLimitResponse(ServerWebExchange exchange, long retryAfter) {
        String message = String.format(
                "Ha excedido el límite de %d solicitudes por minuto. Intente nuevamente en %d segundos.",
                maxRequests, retryAfter);
        String body = String.format(
                "{\"status\":429,\"title\":\"Too Many Requests\",\"detail\":\"%s\",\"type\":\"%s\"}",
                message, URI.create("about:blank"));
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        var addr = exchange.getRequest().getRemoteAddress();
        return addr != null ? addr.getAddress().getHostAddress() : "unknown";
    }
}
