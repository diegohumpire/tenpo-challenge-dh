package com.tenpo.dh.challenge.dhapi.adapter.in.web.mapper;

import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.AuditLogDetailResponse;
import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.AuditLogSummaryResponse;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.CallDirection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class AuditLogResponseMapperTest {

    @Spy
    private AuditLogResponseMapper mapper = new AuditLogResponseMapperImpl();

    @Test
    void toSummaryResponse_mapsAllSummaryFields_andBuildLinks() {
        OffsetDateTime now = OffsetDateTime.now();
        AuditLog log = AuditLog.builder()
                .id(7L)
                .createdAt(now)
                .action("CREATE_CALCULATION")
                .actionType(AuditActionType.HTTP_REQUEST)
                .callDirection(CallDirection.IN)
                .userId("user-1")
                .transactionalId("txn-1")
                .method("POST")
                .endpoint("/api/v1/calculations")
                .params("num1=5")
                .statusCode(201)
                .errorMessage(null)
                .durationMs(42L)
                .requestBody("body")
                .responseBody("resp")
                .build();

        AuditLogSummaryResponse response = mapper.toSummaryResponse(log, "/api/v1/audit-logs/7");

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.action()).isEqualTo("CREATE_CALCULATION");
        assertThat(response.actionType()).isEqualTo(AuditActionType.HTTP_REQUEST);
        assertThat(response.callDirection()).isEqualTo(CallDirection.IN);
        assertThat(response.userId()).isEqualTo("user-1");
        assertThat(response.transactionalId()).isEqualTo("txn-1");
        assertThat(response.method()).isEqualTo("POST");
        assertThat(response.endpoint()).isEqualTo("/api/v1/calculations");
        assertThat(response.params()).isEqualTo("num1=5");
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.durationMs()).isEqualTo(42L);
        assertThat(response.links()).containsKey("detail");
        assertThat(response.links().get("detail").href()).isEqualTo("/api/v1/audit-logs/7");
    }

    @Test
    void toSummaryResponse_doesNotIncludeHeavyFields() {
        AuditLog log = AuditLog.builder()
                .id(1L)
                .action("TEST")
                .actionType(AuditActionType.SYSTEM)
                .requestHeaders("headers")
                .requestBody("body")
                .responseHeaders("resp-headers")
                .responseBody("resp-body")
                .build();

        AuditLogSummaryResponse response = mapper.toSummaryResponse(log, "/api/v1/audit-logs/1");

        // Heavy fields must NOT be present in summary
        assertThat(response).doesNotHaveToString("requestHeaders");
        // Verify _links structure
        assertThat(response.links().get("detail").href()).isEqualTo("/api/v1/audit-logs/1");
    }

    @Test
    void toDetailResponse_mapsAllFields() {
        OffsetDateTime now = OffsetDateTime.now();
        AuditLog log = AuditLog.builder()
                .id(3L)
                .createdAt(now)
                .action("GET_EXTERNAL_PERCENTAGE")
                .actionType(AuditActionType.EXTERNAL_CALL)
                .callDirection(CallDirection.OUT)
                .userId("u1")
                .transactionalId("t1")
                .method("GET")
                .endpoint("https://api.example.com/percentage")
                .params(null)
                .requestHeaders("req-h")
                .requestBody("req-b")
                .responseHeaders("resp-h")
                .responseBody("{\"percentage\":10}")
                .statusCode(200)
                .errorMessage(null)
                .durationMs(100L)
                .build();

        AuditLogDetailResponse response = mapper.toDetailResponse(log);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.action()).isEqualTo("GET_EXTERNAL_PERCENTAGE");
        assertThat(response.requestHeaders()).isEqualTo("req-h");
        assertThat(response.requestBody()).isEqualTo("req-b");
        assertThat(response.responseHeaders()).isEqualTo("resp-h");
        assertThat(response.responseBody()).isEqualTo("{\"percentage\":10}");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.durationMs()).isEqualTo(100L);
    }
}
