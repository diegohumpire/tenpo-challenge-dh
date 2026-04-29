package com.tenpo.dh.challenge.dhapi.application.service;

import com.tenpo.dh.challenge.dhapi.domain.exception.PercentageNotAvailableException;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.CallDirection;
import com.tenpo.dh.challenge.dhapi.domain.model.PercentageCallOutcome;
import com.tenpo.dh.challenge.dhapi.domain.port.out.AuditEventPublisher;
import com.tenpo.dh.challenge.dhapi.domain.port.out.PercentageCacheStore;
import com.tenpo.dh.challenge.dhapi.domain.port.out.PercentageProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PercentageServiceTest {

    @Mock
    private PercentageProvider percentageProvider;

    @Mock
    private PercentageCacheStore percentageCacheStore;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private PercentageService percentageService;

    @Test
    void resolvePercentage_externalServiceSuccess_cachesThenReturnsValue() {
        BigDecimal pct = BigDecimal.valueOf(15);
        when(percentageProvider.getPercentage()).thenReturn(Mono.just(PercentageCallOutcome.of(pct)));
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

    @Test
    void resolvePercentage_externalServiceSuccess_publishesAuditWithHttpMetadata() {
        BigDecimal pct = BigDecimal.valueOf(20);
        PercentageCallOutcome outcome = PercentageCallOutcome.ofHttp(
                pct, "http://external/percentage", "x-mock-percentage=20", "Content-Type=application/json");
        when(percentageProvider.getPercentage()).thenReturn(Mono.just(outcome));
        when(percentageCacheStore.put(pct)).thenReturn(Mono.empty());

        percentageService.resolvePercentage().block();

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditEventPublisher, times(2)).publish(captor.capture());
        List<AuditLog> logs = captor.getAllValues();

        AuditLog externalLog = logs.get(0);
        assertThat(externalLog.getAction()).isEqualTo("GET_EXTERNAL_PERCENTAGE");
        assertThat(externalLog.getActionType()).isEqualTo(AuditActionType.EXTERNAL_CALL);
        assertThat(externalLog.getCallDirection()).isEqualTo(CallDirection.OUT);
        assertThat(externalLog.getEndpoint()).isEqualTo("http://external/percentage");
        assertThat(externalLog.getRequestHeaders()).isEqualTo("x-mock-percentage=20");
        assertThat(externalLog.getResponseHeaders()).isEqualTo("Content-Type=application/json");
        assertThat(externalLog.getStatusCode()).isEqualTo(200);
        assertThat(externalLog.getDurationMs()).isNotNull();

        AuditLog cacheLog = logs.get(1);
        assertThat(cacheLog.getAction()).isEqualTo("CACHE_PUT_PERCENTAGE");
        assertThat(cacheLog.getActionType()).isEqualTo(AuditActionType.CACHE_ACCESS);
    }

    @Test
    void resolvePercentage_externalServiceSuccess_noHttpMetadata_publishesAuditWithNullFields() {
        BigDecimal pct = BigDecimal.valueOf(15);
        when(percentageProvider.getPercentage()).thenReturn(Mono.just(PercentageCallOutcome.of(pct)));
        when(percentageCacheStore.put(pct)).thenReturn(Mono.empty());

        percentageService.resolvePercentage().block();

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditEventPublisher, times(2)).publish(captor.capture());

        AuditLog externalLog = captor.getAllValues().get(0);
        assertThat(externalLog.getEndpoint()).isNull();
        assertThat(externalLog.getRequestHeaders()).isNull();
        assertThat(externalLog.getResponseHeaders()).isNull();
    }

    @Test
    void resolvePercentage_externalServiceFails_publishesExternalFailureAndCacheHitAuditEvents() {
        BigDecimal cachedPct = BigDecimal.valueOf(10);
        when(percentageProvider.getPercentage())
                .thenReturn(Mono.error(new RuntimeException("Service down")));
        when(percentageCacheStore.get()).thenReturn(Mono.just(cachedPct));

        percentageService.resolvePercentage().block();

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditEventPublisher, times(2)).publish(captor.capture());

        AuditLog externalLog = captor.getAllValues().get(0);
        assertThat(externalLog.getAction()).isEqualTo("GET_EXTERNAL_PERCENTAGE");
        assertThat(externalLog.getErrorMessage()).isEqualTo("Service down");

        AuditLog cacheLog = captor.getAllValues().get(1);
        assertThat(cacheLog.getAction()).isEqualTo("CACHE_GET_PERCENTAGE");
        assertThat(cacheLog.getStatusCode()).isEqualTo(200);
    }

    @Test
    void resolvePercentage_externalServiceFails_noCachedValue_publishesCacheMissAuditEvent() {
        when(percentageProvider.getPercentage())
                .thenReturn(Mono.error(new RuntimeException("Service down")));
        when(percentageCacheStore.get()).thenReturn(Mono.empty());

        StepVerifier.create(percentageService.resolvePercentage())
                .expectError(PercentageNotAvailableException.class)
                .verify();

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditEventPublisher, times(2)).publish(captor.capture());

        AuditLog cacheLog = captor.getAllValues().get(1);
        assertThat(cacheLog.getAction()).isEqualTo("CACHE_GET_PERCENTAGE");
        assertThat(cacheLog.getStatusCode()).isEqualTo(404);
    }
}
