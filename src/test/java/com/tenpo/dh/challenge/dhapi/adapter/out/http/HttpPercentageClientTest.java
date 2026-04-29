package com.tenpo.dh.challenge.dhapi.adapter.out.http;

import com.tenpo.dh.challenge.dhapi.config.PercentageProperties;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for retry configuration and circuit breaker behaviour of the percentage HTTP clients.
 * HTTP transport is not tested here; this focuses on resilience mechanics.
 */
class HttpPercentageClientTest {

    private PercentageProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PercentageProperties();
        properties.getRetry().setMaxAttempts(2);
        properties.getRetry().setInitialBackoffSeconds(0);
        properties.getRetry().setMaxBackoffSeconds(0);
    }

    @Test
    void retryProperties_defaultValues_areCorrect() {
        PercentageProperties defaults = new PercentageProperties();
        assertThat(defaults.getRetry().getMaxAttempts()).isEqualTo(3);
        assertThat(defaults.getRetry().getInitialBackoffSeconds()).isEqualTo(1);
        assertThat(defaults.getRetry().getMaxBackoffSeconds()).isEqualTo(5);
    }

    @Test
    void retryProperties_customValues_areReadCorrectly() {
        assertThat(properties.getRetry().getMaxAttempts()).isEqualTo(2);
        assertThat(properties.getRetry().getInitialBackoffSeconds()).isEqualTo(0);
        assertThat(properties.getRetry().getMaxBackoffSeconds()).isEqualTo(0);
    }

    @Test
    void circuitBreaker_defaultProperties_areCorrect() {
        PercentageProperties defaults = new PercentageProperties();
        PercentageProperties.CircuitBreaker cb = defaults.getCircuitBreaker();
        assertThat(cb.getFailureRateThreshold()).isEqualTo(50f);
        assertThat(cb.getSlidingWindowSize()).isEqualTo(10);
        assertThat(cb.getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(cb.getWaitDurationInOpenStateSeconds()).isEqualTo(30);
        assertThat(cb.getPermittedCallsInHalfOpenState()).isEqualTo(3);
    }

    @Test
    void circuitBreaker_opensAfterFailureThreshold() {
        CircuitBreaker cb = CircuitBreakerRegistry.of(
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(100)
                        .slidingWindowSize(3)
                        .minimumNumberOfCalls(3)
                        .waitDurationInOpenState(Duration.ofSeconds(60))
                        .build()
        ).circuitBreaker("test-opens");

        Mono<BigDecimal> failingMono = Mono.<BigDecimal>error(new RuntimeException("down"))
                .transformDeferred(CircuitBreakerOperator.of(cb));

        for (int i = 0; i < 3; i++) {
            StepVerifier.create(failingMono).expectError(RuntimeException.class).verify();
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void circuitBreaker_open_rejectsCallsImmediately() {
        CircuitBreaker cb = CircuitBreakerRegistry.of(
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(100)
                        .slidingWindowSize(1)
                        .minimumNumberOfCalls(1)
                        .waitDurationInOpenState(Duration.ofSeconds(60))
                        .build()
        ).circuitBreaker("test-rejects");

        // Force the circuit open with a single failure
        Mono.<BigDecimal>error(new RuntimeException("first failure"))
                .transformDeferred(CircuitBreakerOperator.of(cb))
                .onErrorComplete()
                .block();

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Next call should be immediately rejected without going through the Mono chain
        StepVerifier.create(
                        Mono.just(new BigDecimal("10"))
                                .transformDeferred(CircuitBreakerOperator.of(cb)))
                .expectError(CallNotPermittedException.class)
                .verify();
    }

    @Test
    void circuitBreaker_halfOpen_allowsTestCall() {
        CircuitBreaker cb = CircuitBreakerRegistry.of(
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(100)
                        .slidingWindowSize(1)
                        .minimumNumberOfCalls(1)
                        .waitDurationInOpenState(Duration.ofMillis(50))
                        .permittedNumberOfCallsInHalfOpenState(1)
                        .build()
        ).circuitBreaker("test-half-open");

        // Open the circuit
        Mono.<BigDecimal>error(new RuntimeException("fail"))
                .transformDeferred(CircuitBreakerOperator.of(cb))
                .onErrorComplete()
                .block();

        // Wait for transition to HALF_OPEN
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        cb.transitionToHalfOpenState();

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        // A successful call in HALF_OPEN should close the circuit
        StepVerifier.create(
                        Mono.just(new BigDecimal("10"))
                                .transformDeferred(CircuitBreakerOperator.of(cb)))
                .expectNextMatches(v -> v.compareTo(new BigDecimal("10")) == 0)
                .verifyComplete();

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}

