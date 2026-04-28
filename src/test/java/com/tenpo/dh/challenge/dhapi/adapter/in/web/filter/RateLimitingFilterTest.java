package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import com.tenpo.dh.challenge.dhapi.config.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitingFilterTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOps;

    @Mock
    private WebFilterChain chain;

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        RateLimitProperties props = new RateLimitProperties();
        props.setMaxRequests(3);
        props.setWindowSeconds(60L);
        filter = new RateLimitingFilter(redisTemplate, props);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void filter_firstRequest_allowsThrough() {
        when(valueOps.increment(anyString())).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/calculations").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void filter_thirdRequest_allowsThrough() {
        when(valueOps.increment(anyString())).thenReturn(Mono.just(3L));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/calculations").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void filter_fourthRequest_returns429Response() {
        when(valueOps.increment(anyString())).thenReturn(Mono.just(4L));
        when(redisTemplate.getExpire(anyString())).thenReturn(Mono.just(Duration.ofSeconds(45)));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/calculations").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assert exchange.getResponse().getStatusCode() != null;
        assert exchange.getResponse().getStatusCode().value() == 429;
    }

    @Test
    void filter_actuatorPath_skipsRateLimiting() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void filter_swaggerPath_skipsRateLimiting() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/swagger-ui.html").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }
}
