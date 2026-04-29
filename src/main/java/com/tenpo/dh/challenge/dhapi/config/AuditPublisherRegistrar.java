package com.tenpo.dh.challenge.dhapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.dh.challenge.dhapi.adapter.out.messaging.KafkaAuditEventPublisher;
import com.tenpo.dh.challenge.dhapi.adapter.out.messaging.SinkAuditEventPublisher;
import com.tenpo.dh.challenge.dhapi.domain.port.in.AuditLogUseCase;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.core.env.Environment;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Programmatically registers the active {@link com.tenpo.dh.challenge.dhapi.domain.port.out.AuditEventPublisher}
 * bean based on the {@code audit.publisher} environment property.
 *
 * <ul>
 *   <li>{@code memory} (default) → {@link SinkAuditEventPublisher} (in-process Reactor Sink)</li>
 *   <li>{@code kafka}            → {@link KafkaAuditEventPublisher} (publishes to Kafka topic)</li>
 * </ul>
 *
 * Imported via {@link AuditPublisherConfig}.
 */
class AuditPublisherRegistrar implements BeanRegistrar {

    @Override
    public void register(BeanRegistry registry, Environment env) {
        String mode = env.getProperty("audit.publisher", "memory");

        if ("kafka".equals(mode)) {
            registerKafkaPublisher(registry, env);
        } else {
            registerMemoryPublisher(registry);
        }
    }

    private void registerKafkaPublisher(BeanRegistry registry, Environment env) {
        String bootstrapServers = env.getProperty("audit.kafka.bootstrap-servers", "localhost:9092");
        String topic = env.getProperty("audit.kafka.topic", "audit-events");

        registry.registerBean(
                "auditEventPublisher",
                KafkaAuditEventPublisher.class,
                spec -> spec.supplier(ctx -> {
                    Map<String, Object> producerProps = new HashMap<>();
                    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                    producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                    producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                    producerProps.put(ProducerConfig.ACKS_CONFIG, "1");

                    KafkaSender<String, String> sender = KafkaSender.create(SenderOptions.create(producerProps));
                    return new KafkaAuditEventPublisher(sender, ctx.bean(ObjectMapper.class), topic);
                }));
    }

    private void registerMemoryPublisher(BeanRegistry registry) {
        registry.registerBean(
                "auditEventPublisher",
                SinkAuditEventPublisher.class,
                spec -> spec.supplier(ctx -> new SinkAuditEventPublisher(ctx.bean(AuditLogUseCase.class))));
    }
}
