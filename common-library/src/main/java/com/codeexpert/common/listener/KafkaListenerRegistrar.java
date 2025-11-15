package com.codeexpert.common.listener;

import com.codeexpert.common.command.BaseCommand;
import com.codeexpert.common.event.BaseEvent;
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
    private final EventSerializer eventSerializer = new JsonEventSerializer();


    public KafkaListenerRegistrar(
            ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory,
            ObjectMapper objectMapper) {
        this.kafkaListenerContainerFactory = kafkaListenerContainerFactory;
        this.objectMapper = objectMapper;
    }

    public void registerListener(
            String topic,
            String groupId,
            DomainEventListener domainEventListener) {

        ConcurrentMessageListenerContainer<String, Object> container =
                kafkaListenerContainerFactory.createContainer(topic);
        container.getContainerProperties().setGroupId(groupId);
        container.getContainerProperties().setMessageListener(new MessageListener<String, String>() {
            @Override
            @SuppressWarnings("unchecked") // Suppress unchecked cast warning
            public void onMessage(ConsumerRecord<String, String> record) {
                try {
                    DomainEvent command = eventSerializer.fromString(record.value());
                    domainEventListener.onEvent(command);
                } catch (Exception e) {
                    log.error("Error processing Kafka message for topic {}: {}", topic, e.getMessage(), e);
                }
            }
        });

        container.start();
        log.info("Registered Kafka listener for topic '{}' with group '{}'", topic, groupId);
    }
}
