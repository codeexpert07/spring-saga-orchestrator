package com.codeexpert.common.publisher;

import com.codeexpert.common.event.DomainEvent; // Changed import
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class KafkaMessagePublisher implements MessagePublisher { // Changed class name and implements MessagePublisher

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public <T extends DomainEvent> void publish(String topic, String key, T message) { // Changed T extends BaseCommand to T extends DomainEvent, and command to message
        log.info("→ Publishing to Kafka topic: {}, key: {}", topic, key);
        kafkaTemplate.send(topic, key, message);
    }
}
