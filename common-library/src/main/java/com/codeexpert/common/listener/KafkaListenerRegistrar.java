package com.codeexpert.common.listener;

import com.codeexpert.common.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Log4j2
@Component
public class KafkaListenerRegistrar {

    private final ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory;
    private final ObjectMapper objectMapper;

    public KafkaListenerRegistrar(
            ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory,
            ObjectMapper objectMapper) {
        this.kafkaListenerContainerFactory = kafkaListenerContainerFactory;
        this.objectMapper = objectMapper;
    }

    public void registerListener(
            String topic,
            String groupId,
            DomainEventListener domainEventListener,
            Class<?> eventType) { // Changed to Class<?>

        ConcurrentMessageListenerContainer<String, Object> container =
                kafkaListenerContainerFactory.createContainer(topic);

        container.getContainerProperties().setGroupId(groupId);
        container.getContainerProperties().setMessageListener(new MessageListener<String, Object>() {
            @Override
            @SuppressWarnings("unchecked") // Suppress unchecked cast warning
            public void onMessage(ConsumerRecord<String, Object> record) {
                try {
                    if (eventType.isInstance(record.value())) {
                        domainEventListener.onEvent((DomainEvent) record.value()); // Cast to DomainEvent
                    } else {
                        log.warn("Received event of unexpected type {} for topic {}. Expected {}.",
                                record.value().getClass().getName(), topic, eventType.getName());
                    }

                } catch (Exception e) {
                    log.error("Error processing Kafka message for topic {}: {}", topic, e.getMessage(), e);
                }
            }
        });

        container.start();
        log.info("Registered Kafka listener for topic '{}' with group '{}' for event type '{}'", topic, groupId, eventType.getSimpleName());
    }
}
