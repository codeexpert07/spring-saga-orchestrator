package org.codeexpert.order.service;

import org.codeexpert.common.event.*;
import org.codeexpert.common.listener.DomainEventListener;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Log4j2
@Service
public class OrderEventListener implements DomainEventListener {

    private final OrderOrchestrator orchestrator;
    private final Map<Class<?>, Consumer<DomainEvent>> eventHandlers; // Changed to Class<?>

    @Autowired
    public OrderEventListener(OrderOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
        this.eventHandlers = new HashMap<>();

        // Register handlers for each event type
        eventHandlers.put(PaymentProcessedEvent.class, this::handlePaymentProcessedEvent);
        eventHandlers.put(PaymentRefundedEvent.class, this::handlePaymentRefundedEvent);
        eventHandlers.put(InventoryReservedEvent.class, this::handleInventoryReservedEvent);
        eventHandlers.put(InventoryReleasedEvent.class, this::handleInventoryReleasedEvent);
        eventHandlers.put(ShipmentCreatedEvent.class, this::handleShipmentCreatedEvent);
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

    private void handlePaymentProcessedEvent(DomainEvent event) {
        orchestrator.handlePaymentEvent((PaymentProcessedEvent) event);
    }

    private void handlePaymentRefundedEvent(DomainEvent event) {
        orchestrator.handlePaymentRefundEvent((PaymentRefundedEvent) event);
    }

    private void handleInventoryReservedEvent(DomainEvent event) {
        orchestrator.handleInventoryEvent((InventoryReservedEvent) event);
    }

    private void handleInventoryReleasedEvent(DomainEvent event) {
        orchestrator.handleInventoryReleaseEvent((InventoryReleasedEvent) event);
    }

    private void handleShipmentCreatedEvent(DomainEvent event) {
        orchestrator.handleShippingEvent((ShipmentCreatedEvent) event);
    }
}