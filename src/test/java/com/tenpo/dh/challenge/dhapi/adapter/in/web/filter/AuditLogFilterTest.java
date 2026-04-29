package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final String TXN_ID = "txn-test-001";
    private static final String USER_ID = "user-99";

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @Mock
    private WebExchangeAuditLogMapper auditLogMapper;

    @Mock
    private WebFilterChain chain;

    private AuditLogFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuditLogFilter(auditEventPublisher, auditLogMapper, new ObjectMapper());
        when(chain.filter(any())).thenReturn(Mono.empty());
        lenient().when(auditLogMapper.toAuditLog(any())).thenReturn(AuditLog.builder().build());
    }

    @Test
    void filter_calculationsPath_buildsContextWithCorrectPathAndPublishes() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/calculations")
                        .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, TXN_ID)
                        .header(RequestHeadersFilter.HEADER_USER_ID, USER_ID)
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        ArgumentCaptor<WebExchangeAuditContext> contextCaptor = ArgumentCaptor.forClass(WebExchangeAuditContext.class);
        verify(auditLogMapper).toAuditLog(contextCaptor.capture());
        verify(auditEventPublisher).publish(any());

        WebExchangeAuditContext capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.path()).isEqualTo("/api/v1/calculations");
        assertThat(capturedContext.method()).isEqualTo("POST");
        assertThat(capturedContext.transactionalId()).isEqualTo(TXN_ID);
        assertThat(capturedContext.userId()).isEqualTo(USER_ID);
    }

    @Test
    void filter_auditLogsPath_buildsContextWithCorrectPathAndPublishes() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/audit-logs")
                        .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, TXN_ID)
                        .header(RequestHeadersFilter.HEADER_USER_ID, USER_ID)
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        ArgumentCaptor<WebExchangeAuditContext> contextCaptor = ArgumentCaptor.forClass(WebExchangeAuditContext.class);
        verify(auditLogMapper).toAuditLog(contextCaptor.capture());
        verify(auditEventPublisher).publish(any());

        assertThat(contextCaptor.getValue().path()).isEqualTo("/api/v1/audit-logs");
    }

    @Test
    void filter_excludedActuatorPath_skipsAuditPublishing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verifyNoInteractions(auditEventPublisher);
        verifyNoInteractions(auditLogMapper);
    }

    @Test
    void filter_excludedSwaggerPath_skipsAuditPublishing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/swagger-ui/index.html").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verifyNoInteractions(auditEventPublisher);
        verifyNoInteractions(auditLogMapper);
    }

    @Test
    void filter_withRequestBody_capturesBodyInContext() {
        String jsonBody = "{\"num1\":5.0,\"num2\":5.0}";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/calculations")
                        .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, TXN_ID)
                        .header(RequestHeadersFilter.HEADER_USER_ID, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jsonBody));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        ArgumentCaptor<WebExchangeAuditContext> contextCaptor = ArgumentCaptor.forClass(WebExchangeAuditContext.class);
        verify(auditLogMapper).toAuditLog(contextCaptor.capture());
        assertThat(contextCaptor.getValue().requestBody()).isEqualTo(jsonBody);
    }

    @Test
    void filter_withPrettyPrintedJsonBody_sanitizesToSingleLine() {
        String prettyJson = "{\n  \"num1\": 5.0,\n  \"num2\": 5.0\n}";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/calculations")
                        .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, TXN_ID)
                        .header(RequestHeadersFilter.HEADER_USER_ID, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(prettyJson));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        ArgumentCaptor<WebExchangeAuditContext> contextCaptor = ArgumentCaptor.forClass(WebExchangeAuditContext.class);
        verify(auditLogMapper).toAuditLog(contextCaptor.capture());

        String captured = contextCaptor.getValue().requestBody();
        assertThat(captured).doesNotContain("\n");
        assertThat(captured).doesNotContain("  ");
    }

    @Test
    void filter_publisherThrows_doesNotPropagateError() {
        doThrow(new RuntimeException("sink closed")).when(auditEventPublisher).publish(any());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/calculations")
                        .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, TXN_ID)
                        .header(RequestHeadersFilter.HEADER_USER_ID, USER_ID)
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }
}

