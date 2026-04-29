package com.tenpo.dh.challenge.dhapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Host configuration for the audit publisher subsystem.
 *
 * <p>Delegates publisher bean selection to {@link AuditPublisherRegistrar} (Spring FW7
 * programmatic registration). Owns cross-cutting infrastructure beans: a lazy
 * {@link KafkaConsumer} factory (conditional on kafka mode) and the {@link ObjectMapper} fallback.
 *
 * <p>The consumer factory is lazy: {@link KafkaConsumer} is constructed only when the poll
 * loop first calls it, preventing eager DNS validation of {@code bootstrap.servers} at startup.
 *
 * <ul>
 *   <li>{@code memory} (default) → {@link com.tenpo.dh.challenge.dhapi.adapter.out.messaging.SinkAuditEventPublisher}</li>
 *   <li>{@code kafka}            → {@link com.tenpo.dh.challenge.dhapi.adapter.out.messaging.KafkaAuditEventPublisher} + Kafka consumer</li>
 * </ul>
 */
@Configuration
@Import(AuditPublisherRegistrar.class)
@EnableConfigurationProperties(AuditPublisherProperties.class)
public class AuditPublisherConfig {

    @Bean
    @ConditionalOnMissingBean
    ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    @ConditionalOnProperty(name = "audit.publisher", havingValue = "kafka")
    Supplier<KafkaConsumer<String, String>> auditKafkaConsumerFactory(AuditPublisherProperties properties) {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                properties.getKafka().getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG,
                properties.getKafka().getConsumerGroup());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        String topic = properties.getKafka().getTopic();
        return () -> {
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
            consumer.subscribe(Collections.singleton(topic));
            return consumer;
        };
    }
}
