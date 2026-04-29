package com.tenpo.dh.challenge.dhapi.config;

import com.tenpo.dh.challenge.dhapi.adapter.out.http.HttpPercentageClient;
import com.tenpo.dh.challenge.dhapi.adapter.out.http.InMemoryPercentageProvider;
import com.tenpo.dh.challenge.dhapi.adapter.out.http.PostmanMockPercentageClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Programmatically registers the active {@link com.tenpo.dh.challenge.dhapi.domain.port.out.PercentageProvider}
 * bean based on the {@code percentage.provider} environment property.
 *
 * <ul>
 *   <li>{@code memory}       → {@link InMemoryPercentageProvider}</li>
 *   <li>{@code postman-mock} → {@link PostmanMockPercentageClient}</li>
 *   <li>anything else        → {@link HttpPercentageClient} (real external call)</li>
 * </ul>
 *
 * Imported via {@link PercentageProviderConfig}.
 */
class PercentageProviderRegistrar implements BeanRegistrar {

    @Override
    public void register(BeanRegistry registry, Environment env) {
        String mode = env.getProperty("percentage.provider", "memory");

        switch (mode) {
            case "memory" -> registry.registerBean(
                    "percentageProvider",
                    InMemoryPercentageProvider.class,
                    spec -> spec.supplier(ctx ->
                            new InMemoryPercentageProvider(ctx.bean(PercentageProperties.class))));

            case "postman-mock" -> registry.registerBean(
                    "percentageProvider",
                    PostmanMockPercentageClient.class,
                    spec -> spec.supplier(ctx ->
                            new PostmanMockPercentageClient(
                                    ctx.bean(WebClient.Builder.class),
                                    ctx.bean(PercentageProperties.class),
                                    ctx.bean(CircuitBreaker.class))));

            default -> registry.registerBean(
                    "percentageProvider",
                    HttpPercentageClient.class,
                    spec -> spec.supplier(ctx ->
                            new HttpPercentageClient(
                                    ctx.bean(WebClient.Builder.class),
                                    ctx.bean(PercentageProperties.class),
                                    ctx.bean(CircuitBreaker.class))));
        }
    }
}
