package com.tenpo.dh.challenge.dhapi;

import com.tenpo.dh.challenge.dhapi.adapter.in.web.filter.RequestHeadersFilter;
import com.tenpo.dh.challenge.dhapi.adapter.out.persistence.AuditLogR2dbcDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

/**
 * Integration test for the Kafka audit publisher mode.
 *
 * Verifies the full flow: HTTP request → KafkaAuditEventPublisher publishes to topic →
 * KafkaAuditLogConsumer consumes → AuditLog persisted to DB → visible via /audit-logs API.
 *
 * Extends {@link AbstractKafkaIntegrationTest} which spins up a real Kafka container and
 * overrides {@code audit.publisher=kafka} and {@code audit.kafka.bootstrap-servers}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class KafkaAuditPublisherIntegrationTest extends AbstractKafkaIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AuditLogR2dbcDao auditLogR2dbcDao;

    @BeforeEach
    void clearAuditLogs() {
        auditLogR2dbcDao.deleteAll().block();
    }

    @Test
    void calculateEndpoint_withKafkaPublisher_auditLogIsPersistedViaKafka() throws InterruptedException {
        webTestClient.post()
                .uri("/api/v1/calculations")
                .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, "txn-kafka-test")
                .header(RequestHeadersFilter.HEADER_USER_ID, "user-kafka-test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("num1", 5.0, "num2", 5.0))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.result").isEqualTo(11.0);

        Thread.sleep(3000); // Allow Kafka produce → consume → DB persist to complete

        webTestClient.get()
                .uri(u -> u.path("/api/v1/audit-logs").queryParam("page", 0).queryParam("size", 20).build())
                .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, "txn-kafka-test")
                .header(RequestHeadersFilter.HEADER_USER_ID, "user-kafka-test")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[?(@.action == 'CREATE_CALCULATION' && @.actionType == 'CALCULATION')]")
                .isNotEmpty();
    }

    @Test
    void calculateEndpoint_withKafkaPublisher_mainResponseIsNotBlocked() {
        webTestClient.post()
                .uri("/api/v1/calculations")
                .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, "txn-kafka-test-2")
                .header(RequestHeadersFilter.HEADER_USER_ID, "user-kafka-test-2")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("num1", 10.0, "num2", 20.0))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.result").isEqualTo(33.0);
    }

    @Test
    void calculateEndpoint_withKafkaPublisher_externalCallAuditLogIsPersisted() throws InterruptedException {
        webTestClient.post()
                .uri("/api/v1/calculations")
                .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, "txn-kafka-test-3")
                .header(RequestHeadersFilter.HEADER_USER_ID, "user-kafka-test-3")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("num1", 1.0, "num2", 2.0))
                .exchange()
                .expectStatus().isCreated();

        Thread.sleep(3000);

        webTestClient.get()
                .uri(u -> u.path("/api/v1/audit-logs").queryParam("page", 0).queryParam("size", 20).build())
                .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, "txn-kafka-test-3")
                .header(RequestHeadersFilter.HEADER_USER_ID, "user-kafka-test-3")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[?(@.action == 'GET_EXTERNAL_PERCENTAGE' && @.actionType == 'EXTERNAL_CALL')]")
                .isNotEmpty();
    }
}
