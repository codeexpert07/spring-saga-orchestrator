package org.codeexpert.shipping;

import org.codeexpert.common.constant.KafkaTopics;
import org.codeexpert.common.listener.DomainEventListener;
import org.codeexpert.common.listener.KafkaListenerRegistrar;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.statemachine.config.EnableStateMachine;

@SpringBootApplication
@EnableStateMachine
public class ShippingServiceApplication {

    private final KafkaListenerRegistrar kafkaListenerRegistrar;
    private final DomainEventListener shippingCommandListener;

    @Autowired
    public ShippingServiceApplication(KafkaListenerRegistrar kafkaListenerRegistrar, DomainEventListener shippingCommandListener) {
        this.kafkaListenerRegistrar = kafkaListenerRegistrar;
        this.shippingCommandListener = shippingCommandListener;
    }

    public static void main(String[] args) {
        SpringApplication.run(ShippingServiceApplication.class, args);
    }

    @PostConstruct
    public void registerKafkaListeners() {
        String groupId = "shipping-service-group"; // Define group ID here or in properties

        kafkaListenerRegistrar.registerListener(
                KafkaTopics.SHIPPING_COMMANDS, groupId, shippingCommandListener);
    }
}
