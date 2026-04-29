package com.tenpo.dh.challenge.dhapi.adapter.out.http;

import com.tenpo.dh.challenge.dhapi.config.PercentageProperties;
import com.tenpo.dh.challenge.dhapi.domain.model.PercentageCallOutcome;
import com.tenpo.dh.challenge.dhapi.domain.port.out.PercentageProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.stream.Collectors;

/**
 * Base abstracta para implementaciones de {@link PercentageProvider} basadas en HTTP.
 * <p>
 * Centraliza la lógica común de timeout, retry, circuit breaker y logging,
 * eliminando la duplicación entre {@link HttpPercentageClient} y
 * {@link PostmanMockPercentageClient}.
 * <p>
 * Las subclases solo necesitan proveer el método {@link #buildRequest()} con el
 * {@code RequestSpec} apropiado para su fuente de datos.
 */
@Slf4j
public abstract class AbstractHttpPercentageClient implements PercentageProvider {

    /**
     * Holds the WebClient request spec together with a summary of any outgoing
     * headers added by the subclass (for audit purposes).
     */
    record RequestSpec(WebClient.RequestHeadersSpec<?> spec, String requestHeadersSummary) {}

    protected final WebClient webClient;
    protected final String baseUrl;
    protected final String path;
    protected final CircuitBreaker circuitBreaker;
    protected final PercentageProperties.Retry retryConfig;
    protected final Duration timeout;

    protected AbstractHttpPercentageClient(WebClient webClient,
                                            String baseUrl,
                                            String path,
                                            CircuitBreaker circuitBreaker,
                                            PercentageProperties properties) {
        this.webClient = webClient;
        this.baseUrl = baseUrl;
        this.path = path;
        this.circuitBreaker = circuitBreaker;
        this.retryConfig = properties.getRetry();
        this.timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
    }

    @Override
    public Mono<PercentageCallOutcome> getPercentage() {
        String endpoint = baseUrl + path;
        return buildRequest()
                .flatMap(requestSpec -> requestSpec.spec().exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        String responseHeaders = headersToString(response.headers().asHttpHeaders());
                        return response.bodyToMono(PercentageResponse.class)
                                .map(body -> PercentageCallOutcome.ofHttp(
                                        body.percentage(),
                                        endpoint,
                                        requestSpec.requestHeadersSummary(),
                                        responseHeaders));
                    }
                    return response.createException().flatMap(Mono::error);
                }))
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
     * @return {@code Mono} con un {@link RequestSpec} listo para ejecutar
     */
    protected abstract Mono<RequestSpec> buildRequest();

    /**
     * Nombre del servicio externo para incluir en logs de retry y error.
     *
     * @return nombre descriptivo del servicio (e.g. "ExternalPercentageService")
     */
    protected abstract String getServiceName();

    private String headersToString(HttpHeaders headers) {
        return headers.toSingleValueMap().entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }
}
