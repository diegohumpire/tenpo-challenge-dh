package com.tenpo.dh.challenge.dhapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "audit")
public class AuditPublisherProperties {

    /** Selects the AuditEventPublisher implementation. Values: memory | kafka */
    private String publisher = "memory";
    private Kafka kafka = new Kafka();

    @Data
    public static class Kafka {
        private String topic = "audit-events";
        private String bootstrapServers = "localhost:9092";
        private String consumerGroup = "dhapi-audit";
    }
}
