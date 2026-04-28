package com.tenpo.dh.challenge.dhapi.adapter.out.http;

import com.tenpo.dh.challenge.dhapi.config.PercentageProperties;
import com.tenpo.dh.challenge.dhapi.domain.port.out.PercentageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * {@link PercentageProvider} backed by a Postman mock server.
 * <p>
 * Headers forwarded from the incoming request to the Postman call:
 * <ul>
 *   <li>{@code x-mock-response-code} — controls which mocked response Postman returns.</li>
 *   <li>{@code x-mock-percentage}    — forwarded so Postman can reflect it in the response.</li>
 * </ul>
 */
@Slf4j
public class PostmanMockPercentageClient implements PercentageProvider {

    private final WebClient webClient;
    private final String path;

    public PostmanMockPercentageClient(WebClient.Builder builder, PercentageProperties properties) {
        this.webClient = builder
                .baseUrl(properties.getPostmanMock().getBaseUrl())
                .build();
        this.path = properties.getPostmanMock().getPath();
    }

    @Override
    public Mono<BigDecimal> getPercentage() {
        return Mono.deferContextual(ctx -> {
            WebClient.RequestHeadersSpec<?> request = webClient.get().uri(path);

            if (ctx.hasKey(ServerWebExchange.class)) {
                HttpHeaders incomingHeaders = ctx.<ServerWebExchange>get(ServerWebExchange.class)
                        .getRequest().getHeaders();

                String mockResponseCode = incomingHeaders.getFirst("x-mock-response-code");
                if (mockResponseCode != null) {
                    request = request.header("x-mock-response-code", mockResponseCode);
                }

                String mockPercentage = incomingHeaders.getFirst("x-mock-percentage");
                if (mockPercentage != null) {
                    request = request.header("x-mock-percentage", mockPercentage);
                }
            }

            return request.retrieve()
                    .bodyToMono(PercentageResponse.class)
                    .map(PercentageResponse::percentage)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(5))
                            .doBeforeRetry(signal -> log.warn(
                                    "Retrying Postman mock call, attempt {}", signal.totalRetries() + 1)))
                    .doOnError(e -> log.error("Postman mock service failed after retries: {}", e.getMessage()));
        });
    }
}
