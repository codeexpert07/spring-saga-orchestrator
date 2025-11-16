package org.codeexpert.order.service;

import org.codeexpert.common.event.*;
import org.codeexpert.order.entity.OrderEntity;
import org.codeexpert.order.model.Order;
import org.codeexpert.order.model.OrderEvent;
import org.codeexpert.order.model.OrderState;
import org.codeexpert.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderOrchestrator {

    private final StateMachineService<OrderState, OrderEvent> stateMachineService;
    private final OrderRepository orderRepository;
    private final Map<String, StateMachine<OrderState, OrderEvent>> stateMachines = new ConcurrentHashMap<>();
    private final Map<String, Order> sagaStore = new ConcurrentHashMap<>();

    @Autowired
    public OrderOrchestrator(StateMachineService<OrderState, OrderEvent> stateMachineService,
                            OrderRepository orderRepository) {
        this.stateMachineService = stateMachineService;
        this.orderRepository = orderRepository;
    }

    public void startSaga(Order order) {
        StateMachine<OrderState, OrderEvent> stateMachine = stateMachineService.acquireStateMachine(order.getOrderId());
        stateMachine.getExtendedState().getVariables().put("order", order);
        stateMachine.start();
        stateMachine.sendEvent(OrderEvent.START_ORDER);
        stateMachines.put(order.getOrderId(), stateMachine);
        sagaStore.put(order.getOrderId(), order);
    }

    public void handlePaymentEvent(PaymentProcessedEvent event) {
        Order order = sagaStore.get(event.getOrderId());
        if (order == null) return;

        StateMachine<OrderState, OrderEvent> stateMachine = stateMachines.get(event.getOrderId());
        if (stateMachine == null) return;

        if ("SUCCESS".equals(event.getStatus())) {
            order.setPaymentTransactionId(event.getTransactionId());
            stateMachine.sendEvent(OrderEvent.PAYMENT_SUCCESS);
        } else {
            order.setErrorMessage(event.getErrorMessage());
            stateMachine.sendEvent(OrderEvent.PAYMENT_FAILED);
        }
    }

    public void handleInventoryEvent(InventoryReservedEvent event) {
        Order order = sagaStore.get(event.getOrderId());
        if (order == null) return;

        StateMachine<OrderState, OrderEvent> stateMachine = stateMachines.get(event.getOrderId());
        if (stateMachine == null) return;

        if ("RESERVED".equals(event.getStatus())) {
            stateMachine.sendEvent(OrderEvent.INVENTORY_RESERVED);
        } else {
            order.setErrorMessage(event.getErrorMessage());
            stateMachine.sendEvent(OrderEvent.INVENTORY_RESERVE_FAILED);
        }
    }

    public void handleShippingEvent(ShipmentCreatedEvent event) {
        Order order = sagaStore.get(event.getOrderId());
        if (order == null) return;

        StateMachine<OrderState, OrderEvent> stateMachine = stateMachines.get(event.getOrderId());
        if (stateMachine == null) return;

        if ("SHIPPED".equals(event.getStatus())) {
            order.setTrackingNumber(event.getTrackingNumber());
            stateMachine.sendEvent(OrderEvent.SHIPPING_SUCCESS);
            updateOrderStatus(event.getOrderId(), OrderState.ORDER_COMPLETED);
        } else {
            order.setErrorMessage(event.getErrorMessage());
            stateMachine.sendEvent(OrderEvent.SHIPPING_FAILED);
        }
    }

    public void handlePaymentRefundEvent(PaymentRefundedEvent event) {
        StateMachine<OrderState, OrderEvent> stateMachine = stateMachines.get(event.getOrderId());
        if (stateMachine != null) {
            stateMachine.sendEvent(OrderEvent.COMPENSATE_PAYMENT);
            updateOrderStatus(event.getOrderId(), OrderState.ORDER_FAILED);
        }
    }

    public void handleInventoryReleaseEvent(InventoryReleasedEvent event) {
        StateMachine<OrderState, OrderEvent> stateMachine = stateMachines.get(event.getOrderId());
        if (stateMachine != null) {
            stateMachine.sendEvent(OrderEvent.COMPENSATE_INVENTORY);
        }
    }

    private void updateOrderStatus(String orderId, OrderState status) {
        OrderEntity order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setState(status);
            orderRepository.save(order);
        }
    }
}
