package org.codeexpert.order.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

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
    public NewTopic paymentEventsTopic() {
        return new NewTopic("payment-events", 1, (short) 1);
    }

    @Bean
    public NewTopic inventoryEventsTopic() {
        return new NewTopic("inventory-events", 1, (short) 1);
    }

    @Bean
    public NewTopic shippingEventsTopic() {
        return new NewTopic("shipping-events", 1, (short) 1);
    }

    @Bean
    public AdminClient adminClient(KafkaAdmin kafkaAdmin) {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }
}
