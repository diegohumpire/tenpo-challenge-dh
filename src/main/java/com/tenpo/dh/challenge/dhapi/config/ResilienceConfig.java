package com.tenpo.dh.challenge.dhapi.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Creates the {@link CircuitBreaker} bean for the percentage provider HTTP clients.
 * <p>
 * Configuration is driven by {@code percentage.circuit-breaker.*} properties.
 */
@Configuration
@EnableConfigurationProperties(PercentageProperties.class)
public class ResilienceConfig {

    public static final String PERCENTAGE_CB = "percentageProvider";

    @Bean
    public CircuitBreaker percentageCircuitBreaker(PercentageProperties properties) {
        PercentageProperties.CircuitBreaker cfg = properties.getCircuitBreaker();

        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(cfg.getFailureRateThreshold())
                .slowCallRateThreshold(cfg.getSlowCallRateThreshold())
                .slowCallDurationThreshold(Duration.ofSeconds(cfg.getSlowCallDurationSeconds()))
                .slidingWindowSize(cfg.getSlidingWindowSize())
                .minimumNumberOfCalls(cfg.getMinimumNumberOfCalls())
                .waitDurationInOpenState(Duration.ofSeconds(cfg.getWaitDurationInOpenStateSeconds()))
                .permittedNumberOfCallsInHalfOpenState(cfg.getPermittedCallsInHalfOpenState())
                .build();

        return CircuitBreakerRegistry.of(cbConfig).circuitBreaker(PERCENTAGE_CB);
    }
}
