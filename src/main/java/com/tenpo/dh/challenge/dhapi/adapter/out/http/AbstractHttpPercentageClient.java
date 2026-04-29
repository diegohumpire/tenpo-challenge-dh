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
 * Base abstracta para implementaciones de {@link PercentageProvider} basadas en HTTP.
 * <p>
 * Centraliza la lógica común de timeout, retry, circuit breaker y logging,
 * eliminando la duplicación entre {@link HttpPercentageClient} y
 * {@link PostmanMockPercentageClient}.
 * <p>
 * Las subclases solo necesitan proveer el método {@link #buildRequest()} con el
 * {@code WebClient.RequestHeadersSpec} apropiado para su fuente de datos.
 */
@Slf4j
public abstract class AbstractHttpPercentageClient implements PercentageProvider {

    protected final WebClient webClient;
    protected final String path;
    protected final CircuitBreaker circuitBreaker;
    protected final PercentageProperties.Retry retryConfig;
    protected final Duration timeout;

    protected AbstractHttpPercentageClient(WebClient webClient,
                                            String path,
                                            CircuitBreaker circuitBreaker,
                                            PercentageProperties properties) {
        this.webClient = webClient;
        this.path = path;
        this.circuitBreaker = circuitBreaker;
        this.retryConfig = properties.getRetry();
        this.timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
    }

    @Override
    public Mono<BigDecimal> getPercentage() {
        return buildRequest()
                .flatMap(request -> request.retrieve()
                        .bodyToMono(PercentageResponse.class))
                .map(PercentageResponse::percentage)
                .timeout(timeout)
                .retryWhen(Retry.backoff(
                                retryConfig.getMaxAttempts(),
                                Duration.ofSeconds(retryConfig.getInitialBackoffSeconds()))
                        .maxBackoff(Duration.ofSeconds(retryConfig.getMaxBackoffSeconds()))
                        .doBeforeRetry(signal -> log.warn(
                                "Retrying {} call, attempt {}/{}",
                                getServiceName(), signal.totalRetries() + 1,
                                retryConfig.getMaxAttempts())))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .doOnError(e -> log.error("{} failed: {}", getServiceName(), e.getMessage()));
    }

    /**
     * Construye la request HTTP específica del cliente.
     * El pipeline de timeout/retry/circuit breaker se aplica automáticamente.
     *
     * @return {@code Mono} con el spec de la request listo para ejecutar
     */
    protected abstract Mono<WebClient.RequestHeadersSpec<?>> buildRequest();

    /**
     * Nombre del servicio externo para incluir en logs de retry y error.
     *
     * @return nombre descriptivo del servicio (e.g. "ExternalPercentageService")
     */
    protected abstract String getServiceName();
}
