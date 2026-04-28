package com.tenpo.dh.challenge.dhapi.adapter.out.http;

import com.tenpo.dh.challenge.dhapi.domain.port.out.PercentageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Component
public class HttpPercentageClient implements PercentageProvider {

    private final WebClient webClient;

    public HttpPercentageClient(WebClient.Builder builder,
                                @Value("${mock.percentage.base-url:http://localhost:8080}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Mono<BigDecimal> getPercentage() {
        return webClient.get()
                .uri("/mock/percentage")
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
