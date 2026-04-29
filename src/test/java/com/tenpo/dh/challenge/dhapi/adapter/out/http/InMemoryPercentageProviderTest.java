package com.tenpo.dh.challenge.dhapi.adapter.out.http;

import com.tenpo.dh.challenge.dhapi.config.PercentageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

class InMemoryPercentageProviderTest {

    private InMemoryPercentageProvider provider;

    @BeforeEach
    void setUp() {
        PercentageProperties properties = new PercentageProperties();
        properties.getInMemory().setValue(BigDecimal.TEN);
        provider = new InMemoryPercentageProvider(properties);
    }

    @Test
    void getPercentage_noContext_returnsConfiguredValue() {
        StepVerifier.create(provider.getPercentage())
                .expectNextMatches(v -> v.compareTo(BigDecimal.TEN) == 0)
                .verifyComplete();
    }

    @Test
    void getPercentage_simulatingFailureEnabled_returnsError() {
        provider.setSimulatingFailure(true);

        StepVerifier.create(provider.getPercentage())
                .expectErrorMessage("Simulated external service failure")
                .verify();
    }

    @Test
    void getPercentage_simulatingFailureResetToFalse_returnsValue() {
        provider.setSimulatingFailure(true);
        provider.setSimulatingFailure(false);

        StepVerifier.create(provider.getPercentage())
                .expectNextMatches(v -> v.compareTo(BigDecimal.TEN) == 0)
                .verifyComplete();
    }
}
