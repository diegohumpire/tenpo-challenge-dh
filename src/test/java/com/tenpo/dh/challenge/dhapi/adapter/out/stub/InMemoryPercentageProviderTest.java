package com.tenpo.dh.challenge.dhapi.adapter.out.stub;

import com.tenpo.dh.challenge.dhapi.config.PercentageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryPercentageProviderTest {

    private InMemoryPercentageProvider provider;
    private MockEnvironment testEnvironment;

    @BeforeEach
    void setUp() {
        PercentageProperties properties = new PercentageProperties();
        properties.getInMemory().setValue(BigDecimal.TEN);

        testEnvironment = new MockEnvironment();
        testEnvironment.setActiveProfiles("test");

        provider = new InMemoryPercentageProvider(properties, testEnvironment);
    }

    @Test
    void getPercentage_noContext_returnsConfiguredValue() {
        StepVerifier.create(provider.getPercentage())
                .assertNext(outcome -> assertThat(outcome.percentage()).isEqualByComparingTo(BigDecimal.TEN))
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
                .assertNext(outcome -> assertThat(outcome.percentage()).isEqualByComparingTo(BigDecimal.TEN))
                .verifyComplete();
    }

    @Test
    void setSimulatingFailure_nonTestProfile_throwsUnsupportedOperationException() {
        MockEnvironment prodEnvironment = new MockEnvironment();
        prodEnvironment.setActiveProfiles("production");

        PercentageProperties properties = new PercentageProperties();
        InMemoryPercentageProvider prodProvider = new InMemoryPercentageProvider(properties, prodEnvironment);

        assertThatThrownBy(() -> prodProvider.setSimulatingFailure(true))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("test");
    }

    @Test
    void setSimulatingFailure_noActiveProfiles_throwsUnsupportedOperationException() {
        MockEnvironment emptyEnvironment = new MockEnvironment();

        PercentageProperties properties = new PercentageProperties();
        InMemoryPercentageProvider provider = new InMemoryPercentageProvider(properties, emptyEnvironment);

        assertThatThrownBy(() -> provider.setSimulatingFailure(true))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

