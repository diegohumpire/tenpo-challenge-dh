package com.tenpo.dh.challenge.dhapi.application.service;

import com.tenpo.dh.challenge.dhapi.domain.exception.PercentageNotAvailableException;
import com.tenpo.dh.challenge.dhapi.domain.port.out.PercentageCacheStore;
import com.tenpo.dh.challenge.dhapi.domain.port.out.PercentageProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PercentageServiceTest {

    @Mock
    private PercentageProvider percentageProvider;

    @Mock
    private PercentageCacheStore percentageCacheStore;

    @InjectMocks
    private PercentageService percentageService;

    @Test
    void resolvePercentage_externalServiceSuccess_cachesThenReturnsValue() {
        BigDecimal pct = BigDecimal.valueOf(15);
        when(percentageProvider.getPercentage()).thenReturn(Mono.just(pct));
        when(percentageCacheStore.put(pct)).thenReturn(Mono.empty());

        StepVerifier.create(percentageService.resolvePercentage())
                .expectNext(pct)
                .verifyComplete();

        verify(percentageCacheStore).put(pct);
    }

    @Test
    void resolvePercentage_externalServiceFails_usesCachedValue() {
        BigDecimal cachedPct = BigDecimal.valueOf(10);
        when(percentageProvider.getPercentage())
                .thenReturn(Mono.error(new RuntimeException("Service down")));
        when(percentageCacheStore.get()).thenReturn(Mono.just(cachedPct));

        StepVerifier.create(percentageService.resolvePercentage())
                .expectNext(cachedPct)
                .verifyComplete();

        verify(percentageCacheStore).get();
    }

    @Test
    void resolvePercentage_externalServiceFails_noCachedValue_throwsException() {
        when(percentageProvider.getPercentage())
                .thenReturn(Mono.error(new RuntimeException("Service down")));
        when(percentageCacheStore.get()).thenReturn(Mono.empty());

        StepVerifier.create(percentageService.resolvePercentage())
                .expectError(PercentageNotAvailableException.class)
                .verify();
    }

    @Test
    void resolvePercentage_doesNotCacheOnExternalFailure() {
        when(percentageProvider.getPercentage())
                .thenReturn(Mono.error(new RuntimeException("Service down")));
        when(percentageCacheStore.get()).thenReturn(Mono.just(BigDecimal.TEN));

        percentageService.resolvePercentage().block();

        verify(percentageCacheStore, never()).put(any());
    }
}
