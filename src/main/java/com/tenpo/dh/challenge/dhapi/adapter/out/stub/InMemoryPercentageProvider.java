package com.tenpo.dh.challenge.dhapi.adapter.out.stub;

import com.tenpo.dh.challenge.dhapi.config.PercentageProperties;
import com.tenpo.dh.challenge.dhapi.domain.port.out.PercentageProvider;
import org.springframework.core.env.Environment;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory implementation of {@link PercentageProvider}.
 * <p>
 * Header behaviour (read from the current reactive request context):
 * <ul>
 * <li>{@code x-mock-response-code}: if present and <b>not</b> a 2xx code →
 * simulates
 * an external service failure ({@code Mono.error}).</li>
 * <li>{@code x-mock-percentage}: if present → overrides the configured value
 * with
 * the supplied number; ignored if not parseable.</li>
 * </ul>
 * When neither header is present the configured
 * {@code percentage.in-memory.value} is returned.
 */
public class InMemoryPercentageProvider implements PercentageProvider {

    private final PercentageProperties properties;
    private final Environment environment;
    private final AtomicBoolean simulatingFailure = new AtomicBoolean(false);

    public InMemoryPercentageProvider(PercentageProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    /**
     * Configures this provider to simulate an external service failure on every
     * call.
     * <p>
     * <b>Only allowed when the {@code test} Spring profile is active.</b>
     * Throws {@link UnsupportedOperationException} in any other environment to
     * prevent accidental invocation in production.
     *
     * @throws UnsupportedOperationException if the {@code test} profile is not active
     */
    public void setSimulatingFailure(boolean value) {
        if (!isTestProfileActive()) {
            throw new UnsupportedOperationException(
                    "setSimulatingFailure is only allowed when the 'test' Spring profile is active");
        }
        this.simulatingFailure.set(value);
    }

    private boolean isTestProfileActive() {
        return Arrays.asList(environment.getActiveProfiles()).contains("test");
    }

    @Override
    public Mono<BigDecimal> getPercentage() {
        if (simulatingFailure.get()) {
            return Mono.error(new RuntimeException("Simulated external service failure"));
        }
        return Mono.deferContextual(ctx -> {
            if (!ctx.hasKey(ServerWebExchange.class)) {
                return Mono.just(properties.getInMemory().getValue());
            }

            ServerHttpRequest request = ctx.<ServerWebExchange>get(ServerWebExchange.class).getRequest();

            // x-mock-response-code: non-2xx forces an error
            String responseCode = request.getHeaders().getFirst("x-mock-response-code");
            if (responseCode != null) {
                try {
                    int code = Integer.parseInt(responseCode.trim());
                    if (code < 200 || code >= 300) {
                        return Mono.error(new RuntimeException(
                                "Simulated service failure via x-mock-response-code: " + code));
                    }
                } catch (NumberFormatException ignored) {
                    // treat invalid code as non-error, continue
                }
            }

            // x-mock-percentage: override the configured value
            String percentageHeader = request.getHeaders().getFirst("x-mock-percentage");
            if (percentageHeader != null) {
                try {
                    return Mono.just(new BigDecimal(percentageHeader.trim()));
                } catch (NumberFormatException ignored) {
                    // fall through to configured value
                }
            }

            return Mono.just(properties.getInMemory().getValue());
        });
    }
}

