package com.tenpo.dh.challenge.dhapi.adapter.out.http;

import com.tenpo.dh.challenge.dhapi.config.PercentageProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link com.tenpo.dh.challenge.dhapi.domain.port.out.PercentageProvider} backed by a Postman mock server.
 * <p>
 * Headers forwarded from the incoming request to the Postman call:
 * <ul>
 *   <li>{@code x-mock-response-code} — controls which mocked response Postman returns.</li>
 *   <li>{@code x-mock-percentage}    — forwarded so Postman can reflect it in the response.</li>
 * </ul>
 */
public class PostmanMockPercentageClient extends AbstractHttpPercentageClient {

    public PostmanMockPercentageClient(WebClient.Builder builder,
                                        PercentageProperties properties,
                                        CircuitBreaker circuitBreaker) {
        super(builder.baseUrl(properties.getPostmanMock().getBaseUrl()).build(),
                properties.getPostmanMock().getBaseUrl(),
                properties.getPostmanMock().getPath(),
                circuitBreaker,
                properties);
    }

    @Override
    protected Mono<RequestSpec> buildRequest() {
        return Mono.deferContextual(ctx -> {
            WebClient.RequestHeadersSpec<?> request = webClient.get().uri(path);
            Map<String, String> addedHeaders = new LinkedHashMap<>();

            if (ctx.hasKey(ServerWebExchange.class)) {
                HttpHeaders incomingHeaders = ctx.<ServerWebExchange>get(ServerWebExchange.class)
                        .getRequest().getHeaders();

                String mockResponseCode = incomingHeaders.getFirst("x-mock-response-code");
                if (mockResponseCode != null) {
                    request = request.header("x-mock-response-code", mockResponseCode);
                    addedHeaders.put("x-mock-response-code", mockResponseCode);
                }

                String mockPercentage = incomingHeaders.getFirst("x-mock-percentage");
                if (mockPercentage != null) {
                    request = request.header("x-mock-percentage", mockPercentage);
                    addedHeaders.put("x-mock-percentage", mockPercentage);
                }
            }

            String headersSummary = addedHeaders.isEmpty() ? null
                    : addedHeaders.entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .collect(Collectors.joining(", "));

            return Mono.just(new RequestSpec(request, headersSummary));
        });
    }

    @Override
    protected String getServiceName() {
        return "PostmanMockPercentageService";
    }
}

