package org.codeexpert.order.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

import static org.codeexpert.common.constant.KafkaTopics.PAYMENT_COMMANDS;

/**
 * Configuration for Kafka in embedded mode (for development only).
 */
@Configuration
public class EmbeddedKafkaBootstrapConfiguration {

    @Value("${kafka.embedded.broker.host:localhost}")
    private String brokerHost;

    @Value("${kafka.embedded.broker.port:9092}")
    private int brokerPort;


    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, 
                   String.format("%s:%d", brokerHost, brokerPort));
        return new KafkaAdmin(configs);
    }

    @Bean
    public AdminClient adminClient(KafkaAdmin kafkaAdmin) {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(KafkaProperties properties) {
        return new DefaultKafkaProducerFactory<>(
                properties.buildProducerProperties(),
                new JsonSerializer<>(),
                new JsonSerializer<>()
        );
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name("payment-events").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryEventsTopic() {
        return TopicBuilder.name("inventory-events").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic shippingEventsTopic() {
        return TopicBuilder.name("shipping-events").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic paymentCommandTopic() {
        return TopicBuilder.name(PAYMENT_COMMANDS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryCommandsTopic() {
        return TopicBuilder.name("inventory-commands").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic shippingCommandsTopic() {
        return TopicBuilder.name("shipping-commands").partitions(1).replicas(1).build();
    }
}
