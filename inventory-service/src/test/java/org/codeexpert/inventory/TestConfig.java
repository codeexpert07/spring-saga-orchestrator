package org.codeexpert.inventory;

import com.codeexpert.common.listener.KafkaListenerRegistrar;
import com.codeexpert.common.publisher.KafkaMessagePublisher;
import com.codeexpert.common.publisher.MessagePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public KafkaListenerRegistrar kafkaListenerRegistrar(ObjectMapper objectMapper, ConcurrentKafkaListenerContainerFactory<String, Object> factory) {
        // Mock this one since it's not needed for the test
        return new KafkaListenerRegistrar(factory, objectMapper);
    }

    @Bean
    @Primary
    public MessagePublisher messagePublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        // Create a real MessagePublisher that actually publishes to Kafka
        return new KafkaMessagePublisher();
    }
}
