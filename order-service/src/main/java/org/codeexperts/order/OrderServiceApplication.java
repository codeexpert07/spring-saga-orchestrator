package org.codeexperts.order;

import com.codeexpert.common.constant.KafkaTopics;
import com.codeexpert.common.event.*;
import com.codeexpert.common.listener.DomainEventListener;
import com.codeexpert.common.listener.KafkaListenerRegistrar;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.statemachine.config.EnableStateMachine;

@SpringBootApplication
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
        SpringApplication.run(SpringApplication.class, args);
    }

    @PostConstruct
    public void registerKafkaListeners() {
        String groupId = "order-service-group"; // Define group ID here or in properties

        kafkaListenerRegistrar.registerListener(
                KafkaTopics.PAYMENT_EVENTS, groupId, orderEventListener, PaymentProcessedEvent.class);
        kafkaListenerRegistrar.registerListener(
                KafkaTopics.PAYMENT_EVENTS, groupId, orderEventListener, PaymentRefundedEvent.class);
        kafkaListenerRegistrar.registerListener(
                KafkaTopics.INVENTORY_EVENTS, groupId, orderEventListener, InventoryReservedEvent.class);
        kafkaListenerRegistrar.registerListener(
                KafkaTopics.INVENTORY_EVENTS, groupId, orderEventListener, InventoryReleasedEvent.class);
        kafkaListenerRegistrar.registerListener(
                KafkaTopics.SHIPPING_EVENTS, groupId, orderEventListener, ShipmentCreatedEvent.class);
    }
}
