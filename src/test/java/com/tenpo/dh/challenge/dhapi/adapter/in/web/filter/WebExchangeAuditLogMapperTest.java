package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.CallDirection;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static java.time.temporal.ChronoUnit.SECONDS;

class WebExchangeAuditLogMapperTest {

    private final WebExchangeAuditLogMapper mapper = new WebExchangeAuditLogMapperImpl();

    // ── resolveAction ──────────────────────────────────────────────────────────

    @Test
    void resolveAction_calculationsPath_returnsCreateCalculation() {
        WebExchangeAuditContext ctx = context("/api/v1/calculations", "POST");
        assertThat(mapper.resolveAction(ctx)).isEqualTo("CREATE_CALCULATION");
    }

    @Test
    void resolveAction_auditLogsPath_returnsGetAuditLogs() {
        WebExchangeAuditContext ctx = context("/api/v1/audit-logs", "GET");
        assertThat(mapper.resolveAction(ctx)).isEqualTo("GET_AUDIT_LOGS");
    }

    @Test
    void resolveAction_unknownPath_returnsMethodAndNormalisedPath() {
        WebExchangeAuditContext ctx = context("/api/v1/some-resource", "GET");
        assertThat(mapper.resolveAction(ctx)).isEqualTo("GET__API_V1_SOME_RESOURCE");
    }

    // ── resolveActionType ──────────────────────────────────────────────────────

    @Test
    void resolveActionType_calculationsPath_returnsCalculation() {
        assertThat(mapper.resolveActionType("/api/v1/calculations")).isEqualTo(AuditActionType.CALCULATION);
    }

    @Test
    void resolveActionType_auditLogsPath_returnsHttpRequest() {
        assertThat(mapper.resolveActionType("/api/v1/audit-logs")).isEqualTo(AuditActionType.HTTP_REQUEST);
    }

    @Test
    void resolveActionType_unknownPath_returnsHttpRequest() {
        assertThat(mapper.resolveActionType("/api/v1/other")).isEqualTo(AuditActionType.HTTP_REQUEST);
    }

    // ── toAuditLog ─────────────────────────────────────────────────────────────

    @Test
    void toAuditLog_calculationsContext_setsCalculationActionType() {
        WebExchangeAuditContext ctx = context("/api/v1/calculations", "POST");
        AuditLog log = mapper.toAuditLog(ctx);
        assertThat(log.getActionType()).isEqualTo(AuditActionType.CALCULATION);
        assertThat(log.getAction()).isEqualTo("CREATE_CALCULATION");
    }

    @Test
    void toAuditLog_setsCallDirectionAsIn() {
        AuditLog log = mapper.toAuditLog(context("/api/v1/calculations", "POST"));
        assertThat(log.getCallDirection()).isEqualTo(CallDirection.IN);
    }

    @Test
    void toAuditLog_setsCreatedAtToApproximatelyNow() {
        AuditLog log = mapper.toAuditLog(context("/api/v1/calculations", "POST"));
        assertThat(log.getCreatedAt()).isCloseTo(OffsetDateTime.now(), within(5, SECONDS));
    }

    @Test
    void toAuditLog_mapsPathToEndpoint() {
        WebExchangeAuditContext ctx = context("/api/v1/calculations", "POST");
        assertThat(mapper.toAuditLog(ctx).getEndpoint()).isEqualTo("/api/v1/calculations");
    }

    @Test
    void toAuditLog_mapsAllScalarFields() {
        WebExchangeAuditContext ctx = new WebExchangeAuditContext(
                "txn-123", "user-42", "/api/v1/calculations", "POST",
                "num1=5", "{Authorization: Bearer token}", "{\"num1\":5}",
                "{Content-Type: application/json}", "{\"result\":11.0}", 201, 42L);

        AuditLog log = mapper.toAuditLog(ctx);

        assertThat(log.getTransactionalId()).isEqualTo("txn-123");
        assertThat(log.getUserId()).isEqualTo("user-42");
        assertThat(log.getMethod()).isEqualTo("POST");
        assertThat(log.getParams()).isEqualTo("num1=5");
        assertThat(log.getRequestHeaders()).isEqualTo("{Authorization: Bearer token}");
        assertThat(log.getRequestBody()).isEqualTo("{\"num1\":5}");
        assertThat(log.getResponseHeaders()).isEqualTo("{Content-Type: application/json}");
        assertThat(log.getResponseBody()).isEqualTo("{\"result\":11.0}");
        assertThat(log.getStatusCode()).isEqualTo(201);
        assertThat(log.getDurationMs()).isEqualTo(42L);
    }

    @Test
    void toAuditLog_mapsResponseBodyAndHeaders() {
        WebExchangeAuditContext ctx = new WebExchangeAuditContext(
                "txn-1", "user-1", "/api/v1/calculations", "POST",
                null, null, "",
                "{X-Transactional-Id: txn-1}", "{\"result\":11.0}", 201, 10L);

        AuditLog log = mapper.toAuditLog(ctx);

        assertThat(log.getResponseHeaders()).isEqualTo("{X-Transactional-Id: txn-1}");
        assertThat(log.getResponseBody()).isEqualTo("{\"result\":11.0}");
    }

    @Test
    void toAuditLog_ignoresUnmappedAuditLogFields() {
        AuditLog log = mapper.toAuditLog(context("/api/v1/calculations", "POST"));
        assertThat(log.getId()).isNull();
        assertThat(log.getErrorMessage()).isNull();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private WebExchangeAuditContext context(String path, String method) {
        return new WebExchangeAuditContext("txn-id", "user-1", path, method, null, null, "", null, null, null, 0L);
    }
}
