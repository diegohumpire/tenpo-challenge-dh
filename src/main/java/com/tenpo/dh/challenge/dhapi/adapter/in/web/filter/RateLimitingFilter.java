package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import com.tenpo.dh.challenge.dhapi.config.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitingFilter implements WebFilter {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RateLimitProperties rateLimitProperties;
    private final FilterResponseWriter filterResponseWriter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (FilterExclusionConfig.isExcluded(path)) {
            return chain.filter(exchange);
        }

        String clientIp = resolveClientIp(exchange);
        String key = "rate_limit:" + clientIp;

        return redisTemplate.opsForValue().increment(key).flatMap(count -> {
            if (count == 1) {
                return redisTemplate.expire(key, Duration.ofSeconds(rateLimitProperties.getWindowSeconds())).thenReturn(count);
            }
            return Mono.just(count);
        }).flatMap(count -> {
            long remaining = Math.max(0, rateLimitProperties.getMaxRequests() - count);

            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(rateLimitProperties.getMaxRequests()));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(remaining));

            if (count > rateLimitProperties.getMaxRequests()) {
                log.warn("Rate limit exceeded for IP={}, count={}", clientIp, count);
                return redisTemplate.getExpire(key).flatMap(ttl -> {
                    long retryAfter = ttl.getSeconds() > 0 ? ttl.getSeconds() : rateLimitProperties.getWindowSeconds();
                    exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(retryAfter));
                    exchange.getResponse().getHeaders().add("X-RateLimit-Reset",
                            String.valueOf(System.currentTimeMillis() / 1000 + retryAfter));

                    String detail = String.format(
                            "Ha excedido el límite de %d solicitudes por minuto. Intente nuevamente en %d segundos.",
                            rateLimitProperties.getMaxRequests(), retryAfter);
                    return filterResponseWriter.writeTooManyRequests(exchange, detail);
                });
            }

            return chain.filter(exchange);
        });
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
