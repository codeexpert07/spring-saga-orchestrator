package org.codeexpert.shipping;

import org.codeexpert.common.constant.KafkaTopics;
import org.codeexpert.common.listener.KafkaListenerRegistrar;
import org.codeexpert.common.publisher.KafkaMessagePublisher;
import org.codeexpert.common.publisher.MessagePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;

@TestConfiguration
public class TestConfig {

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9094");
        String bootstrapServers = (String) configs.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG);
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        configs.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);

        KafkaAdmin admin = new KafkaAdmin(configs);
        admin.setAutoCreate(true);

        System.out.println("✓ Shipping Service KafkaAdmin configured with bootstrap servers: " + bootstrapServers);
        return admin;
    }

    /**
     * Pre-create topics to ensure they exist before consumers start
     */
    @Bean
    public NewTopic shippingCommandsTopic() {
        return TopicBuilder.name(KafkaTopics.SHIPPING_COMMANDS)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic shippingEventsTopic() {
        return TopicBuilder.name(KafkaTopics.SHIPPING_EVENTS)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    @Primary
    public MessagePublisher messagePublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaMessagePublisher();
    }

    @Bean
    @Primary
    public KafkaListenerRegistrar kafkaListenerRegistrar(
            ConcurrentKafkaListenerContainerFactory<String, Object> factory,
            ObjectMapper objectMapper) {
        return new KafkaListenerRegistrar(factory, objectMapper);
    }
}
