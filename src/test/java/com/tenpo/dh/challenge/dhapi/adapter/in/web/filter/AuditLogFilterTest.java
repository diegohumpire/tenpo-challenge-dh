package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.port.out.AuditEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogFilterTest {

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @Mock
    private WebFilterChain chain;

    private AuditLogFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuditLogFilter(auditEventPublisher);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void filter_calculationsPath_publishesAuditLogWithCalculationActionType() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/calculations").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditEventPublisher).publish(captor.capture());
        AuditLog published = captor.getValue();

        assertThat(published.getActionType()).isEqualTo(AuditActionType.CALCULATION);
        assertThat(published.getAction()).isEqualTo("CREATE_CALCULATION");
    }

    @Test
    void filter_auditLogsPath_publishesAuditLogWithHttpRequestActionType() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/audit-logs").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditEventPublisher).publish(captor.capture());
        AuditLog published = captor.getValue();

        assertThat(published.getActionType()).isEqualTo(AuditActionType.HTTP_REQUEST);
        assertThat(published.getAction()).isEqualTo("GET_AUDIT_LOGS");
    }

    @Test
    void filter_excludedActuatorPath_skipsAuditPublishing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verifyNoInteractions(auditEventPublisher);
    }

    @Test
    void filter_excludedSwaggerPath_skipsAuditPublishing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/swagger-ui/index.html").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verifyNoInteractions(auditEventPublisher);
    }

    @Test
    void filter_withRequestBody_capturesBodyInAuditLog() {
        String jsonBody = "{\"num1\":5.0,\"num2\":5.0}";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jsonBody));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getRequestBody()).isEqualTo(jsonBody);
    }

    @Test
    void filter_publisherThrows_doesNotPropagateError() {
        doThrow(new RuntimeException("sink closed")).when(auditEventPublisher).publish(any());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/calculations").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }
}
