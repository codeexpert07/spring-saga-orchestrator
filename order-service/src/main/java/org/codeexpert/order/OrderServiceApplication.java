package org.codeexpert.order;

import org.codeexpert.common.constant.KafkaTopics;
import org.codeexpert.common.listener.DomainEventListener;
import org.codeexpert.common.listener.KafkaListenerRegistrar;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.statemachine.config.EnableStateMachine;

@SpringBootApplication( scanBasePackages = {
        "org.codeexpert.order",
        "org.codeexpert.common"  // Make sure this package is scanned
})
@EnableStateMachine
public class OrderServiceApplication {

    private final KafkaListenerRegistrar kafkaListenerRegistrar;
    private final DomainEventListener orderEventListener;

    @Autowired
    public OrderServiceApplication(KafkaListenerRegistrar kafkaListenerRegistrar, DomainEventListener orderEventListener) {
        this.kafkaListenerRegistrar = kafkaListenerRegistrar;
        this.orderEventListener = orderEventListener;
    }

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

    @PostConstruct
    public void registerKafkaListeners() {
        String groupId = "order-service-group"; // Define group ID here or in properties

        kafkaListenerRegistrar.registerListener(
                KafkaTopics.PAYMENT_EVENTS, groupId, orderEventListener);
        kafkaListenerRegistrar.registerListener(
                KafkaTopics.PAYMENT_EVENTS, groupId, orderEventListener);
        kafkaListenerRegistrar.registerListener(
                KafkaTopics.INVENTORY_EVENTS, groupId, orderEventListener);
        kafkaListenerRegistrar.registerListener(
                KafkaTopics.INVENTORY_EVENTS, groupId, orderEventListener);
        kafkaListenerRegistrar.registerListener(
                KafkaTopics.SHIPPING_EVENTS, groupId, orderEventListener);
    }
}
