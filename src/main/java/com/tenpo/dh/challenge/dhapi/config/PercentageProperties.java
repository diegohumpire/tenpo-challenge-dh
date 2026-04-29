package com.tenpo.dh.challenge.dhapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Data
@ConfigurationProperties(prefix = "percentage")
public class PercentageProperties {

    private String provider = "memory";
    private long timeoutSeconds = 10;
    private Cache cache = new Cache();
    private InMemory inMemory = new InMemory();
    private PostmanMock postmanMock = new PostmanMock();
    private External external = new External();
    private Retry retry = new Retry();
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    @Data
    public static class Cache {
        /** Cache TTL in seconds. Default: 1800 (30 minutes). */
        private long ttl = 1800;
    }

    @Data
    public static class InMemory {
        /** Default percentage value returned when no x-mock-percentage header is present. */
        private BigDecimal value = BigDecimal.TEN;
    }

    @Data
    public static class PostmanMock {
        private String baseUrl = "https://ec995055-c0c3-4482-aa85-89f5660540f0.mock.pstmn.io";
        private String path = "/mock/percentage";
    }

    @Data
    public static class External {
        private String baseUrl = "http://localhost:8080";
        private String path = "/percentage";
    }

    @Data
    public static class Retry {
        /** Maximum number of retry attempts after the first failure. */
        private int maxAttempts = 3;
        /** Initial backoff duration in seconds. */
        private long initialBackoffSeconds = 1;
        /** Maximum backoff duration in seconds. */
        private long maxBackoffSeconds = 5;
    }

    @Data
    public static class CircuitBreaker {
        /** Failure rate threshold (%) to transition to OPEN. */
        private float failureRateThreshold = 50;
        /** Slow call rate threshold (%) to transition to OPEN. */
        private float slowCallRateThreshold = 100;
        /** Duration in seconds above which a call is considered slow. */
        private long slowCallDurationSeconds = 3;
        /** Number of calls in the sliding window used to evaluate the circuit. */
        private int slidingWindowSize = 10;
        /** Minimum number of calls before the circuit breaker can evaluate failure rate. */
        private int minimumNumberOfCalls = 5;
        /** Time in seconds the circuit stays OPEN before transitioning to HALF_OPEN. */
        private long waitDurationInOpenStateSeconds = 30;
        /** Number of calls allowed through in HALF_OPEN state. */
        private int permittedCallsInHalfOpenState = 3;
    }
}
