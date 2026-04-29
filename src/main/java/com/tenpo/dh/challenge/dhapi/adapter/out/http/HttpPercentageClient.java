package com.tenpo.dh.challenge.dhapi.adapter.out.http;

import com.tenpo.dh.challenge.dhapi.config.PercentageProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * {@link com.tenpo.dh.challenge.dhapi.domain.port.out.PercentageProvider} que delega a un servicio HTTP externo real.
 * Configurado via {@code percentage.external.*} properties.
 * No aplica lógica de mock-headers.
 */
public class HttpPercentageClient extends AbstractHttpPercentageClient {

    public HttpPercentageClient(WebClient.Builder builder,
                                PercentageProperties properties,
                                CircuitBreaker circuitBreaker) {
        super(builder.baseUrl(properties.getExternal().getBaseUrl()).build(),
                properties.getExternal().getPath(),
                circuitBreaker,
                properties);
    }

    @Override
    protected Mono<WebClient.RequestHeadersSpec<?>> buildRequest() {
        return Mono.just(webClient.get().uri(path));
    }

    @Override
    protected String getServiceName() {
        return "ExternalPercentageService";
    }
}

