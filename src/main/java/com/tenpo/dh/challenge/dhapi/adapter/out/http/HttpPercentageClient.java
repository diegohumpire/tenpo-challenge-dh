package com.tenpo.dh.challenge.dhapi.adapter.out.http;

import com.tenpo.dh.challenge.dhapi.config.PercentageProperties;
import com.tenpo.dh.challenge.dhapi.domain.port.out.PercentageProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * {@link PercentageProvider} that delegates to a real external HTTP service.
 * Configured via {@code percentage.external.*} properties.
 * Does not apply any mock-header logic.
 */
@Slf4j
public class HttpPercentageClient implements PercentageProvider {

    private final WebClient webClient;
    private final String path;
    private final CircuitBreaker circuitBreaker;
    private final PercentageProperties.Retry retryConfig;

    public HttpPercentageClient(WebClient.Builder builder,
                                PercentageProperties properties,
                                CircuitBreaker circuitBreaker) {
        this.webClient = builder.baseUrl(properties.getExternal().getBaseUrl()).build();
        this.path = properties.getExternal().getPath();
        this.circuitBreaker = circuitBreaker;
        this.retryConfig = properties.getRetry();
    }

    @Override
    public Mono<BigDecimal> getPercentage() {
        return webClient.get()
                .uri(path)
                .retrieve()
                .bodyToMono(PercentageResponse.class)
                .map(PercentageResponse::percentage)
                .retryWhen(Retry.backoff(retryConfig.getMaxAttempts(), Duration.ofSeconds(retryConfig.getInitialBackoffSeconds()))
                        .maxBackoff(Duration.ofSeconds(retryConfig.getMaxBackoffSeconds()))
                        .doBeforeRetry(signal -> log.warn("Retrying external percentage call, attempt {}",
                                signal.totalRetries() + 1)))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .doOnError(e -> log.error("External percentage service failed: {}", e.getMessage()));
    }
}

