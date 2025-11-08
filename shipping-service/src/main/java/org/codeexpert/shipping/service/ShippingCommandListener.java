package org.codeexpert.shipping.service;

import com.codeexpert.common.command.CreateShipmentCommand;
import com.codeexpert.common.constant.KafkaTopics;
import com.codeexpert.common.event.DomainEvent;
import com.codeexpert.common.event.ShipmentCreatedEvent;
import com.codeexpert.common.listener.DomainEventListener;
import com.codeexpert.common.publisher.MessagePublisher; // Changed import
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Log4j2
@Service
public class ShippingCommandListener implements DomainEventListener {

    private final MessagePublisher messagePublisher; // Changed injected type
    private final Map<Class<?>, Consumer<DomainEvent>> eventHandlers;

    @Autowired
    public ShippingCommandListener(MessagePublisher messagePublisher) { // Changed injected type
        this.messagePublisher = messagePublisher;
        this.eventHandlers = new HashMap<>();

        // Register handlers for each command/event type this service listens to
        eventHandlers.put(CreateShipmentCommand.class, this::handleCreateShipmentCommand);
    }

    @Override
    public void onEvent(DomainEvent event) {
        Consumer<DomainEvent> handler = eventHandlers.get(event.getClass());
        if (handler != null) {
            handler.accept(event);
        } else {
            log.warn("No handler found for event type: {}", event.getClass().getSimpleName());
        }
    }

    @Transactional
    private void handleCreateShipmentCommand(DomainEvent domainEvent) {
        CreateShipmentCommand command = (CreateShipmentCommand) domainEvent;
        try {
            System.out.println("🚚 Shipping Service: Creating shipment for " + command.getOrderId());

            ShipmentCreatedEvent event = ShipmentCreatedEvent.builder()
                    .orderId(command.getOrderId())
                    .shipmentId(UUID.randomUUID().toString())
                    .status("SUCCESS") // Assuming success by default
                    .correlationId(command.getCorrelationId())
                    .build();

            messagePublisher.publish(KafkaTopics.SHIPPING_EVENTS, event.getOrderId(), event); // Changed commandPublisher to messagePublisher

        } catch (Exception e) {
            ShipmentCreatedEvent event = ShipmentCreatedEvent.builder()
                    .orderId(command.getOrderId())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .correlationId(command.getCorrelationId())
                    .build();

            messagePublisher.publish(KafkaTopics.SHIPPING_EVENTS, event.getOrderId(), event); // Changed commandPublisher to messagePublisher
        }
    }
}
