package com.tenpo.dh.challenge.dhapi.adapter.out.http;

import com.tenpo.dh.challenge.dhapi.config.PercentageProperties;
import com.tenpo.dh.challenge.dhapi.domain.port.out.PercentageProvider;
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

    public HttpPercentageClient(WebClient.Builder builder, PercentageProperties properties) {
        this.webClient = builder.baseUrl(properties.getExternal().getBaseUrl()).build();
        this.path = properties.getExternal().getPath();
    }

    @Override
    public Mono<BigDecimal> getPercentage() {
        return webClient.get()
                .uri(path)
                .retrieve()
                .bodyToMono(PercentageResponse.class)
                .map(PercentageResponse::percentage)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(5))
                        .doBeforeRetry(signal -> log.warn("Retrying external percentage call, attempt {}",
                                signal.totalRetries() + 1)))
                .doOnError(e -> log.error("External percentage service failed after retries: {}", e.getMessage()));
    }
}
