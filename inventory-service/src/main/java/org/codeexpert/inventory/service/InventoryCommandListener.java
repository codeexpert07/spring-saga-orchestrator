package org.codeexpert.inventory.service;

import org.codeexpert.common.command.ReleaseInventoryCommand;
import org.codeexpert.common.command.ReserveInventoryCommand;
import org.codeexpert.common.constant.KafkaTopics;
import org.codeexpert.common.event.DomainEvent;
import org.codeexpert.common.event.InventoryReleasedEvent;
import org.codeexpert.common.event.InventoryReservedEvent;
import org.codeexpert.common.listener.DomainEventListener;
import org.codeexpert.common.publisher.MessagePublisher; // Changed import
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
public class InventoryCommandListener implements DomainEventListener {

    private final MessagePublisher messagePublisher; // Changed injected type
    private final Map<Class<?>, Consumer<DomainEvent>> eventHandlers;

    @Autowired
    public InventoryCommandListener(MessagePublisher messagePublisher) { // Changed injected type
        this.messagePublisher = messagePublisher;
        this.eventHandlers = new HashMap<>();

        // Register handlers for each command/event type this service listens to
        eventHandlers.put(ReserveInventoryCommand.class, this::handleReserveInventoryCommand);
        eventHandlers.put(ReleaseInventoryCommand.class, this::handleReleaseInventoryCommand);
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
    private void handleReserveInventoryCommand(DomainEvent domainEvent) {
        ReserveInventoryCommand command = (ReserveInventoryCommand) domainEvent;
        log.info("📦 Inventory Service: Reserving items for {}", command.getOrderId());

        try {
            InventoryReservedEvent event = InventoryReservedEvent.builder()
                    .orderId(command.getOrderId())
                    .reservationId(UUID.randomUUID().toString())
                    .status("SUCCESS")
                    .correlationId(command.getCorrelationId())
                    .build();

            messagePublisher.publish(KafkaTopics.INVENTORY_EVENTS, event.getOrderId(), event); // Changed commandPublisher to messagePublisher

        } catch (Exception e) {
            InventoryReservedEvent event = InventoryReservedEvent.builder()
                    .orderId(command.getOrderId())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .correlationId(command.getCorrelationId())
                    .build();

            messagePublisher.publish(KafkaTopics.INVENTORY_EVENTS, event.getOrderId(), event); // Changed commandPublisher to messagePublisher
        }
    }

    @Transactional
    private void handleReleaseInventoryCommand(DomainEvent domainEvent) {
        ReleaseInventoryCommand command = (ReleaseInventoryCommand) domainEvent;
        System.out.println("↻ Inventory Service: Releasing " + command.getReservationId());

        InventoryReleasedEvent event = InventoryReleasedEvent.builder()
                .orderId(command.getOrderId())
                .reservationId(command.getReservationId())
                .correlationId(command.getCorrelationId())
                .status("SUCCESS")
                .build();

        messagePublisher.publish(KafkaTopics.INVENTORY_EVENTS, event.getOrderId(), event); // Changed commandPublisher to messagePublisher
    }
}
