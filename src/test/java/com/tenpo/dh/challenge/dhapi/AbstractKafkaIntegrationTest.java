package com.tenpo.dh.challenge.dhapi;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Base class for integration tests that require a running Kafka broker.
 * Extends {@link AbstractIntegrationTest} so Postgres and Redis are also available.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractKafkaIntegrationTest extends AbstractIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    protected static final KafkaContainer kafka =
            new KafkaContainer("apache/kafka-native:4.0.0");

    @DynamicPropertySource
    static void overrideKafkaProperties(DynamicPropertyRegistry registry) {
        if (!kafka.isRunning()) {
            return;
        }
        registry.add("audit.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("audit.publisher", () -> "kafka");
    }
}
