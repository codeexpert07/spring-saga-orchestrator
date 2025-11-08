package org.codeexperts.order.service;

import com.codeexpert.common.event.*;
import org.codeexperts.order.entity.OrderEntity;
import org.codeexperts.order.model.Order;
import org.codeexperts.order.model.OrderEvent;
import org.codeexperts.order.model.OrderState;
import org.codeexperts.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderOrchestrator {

    @Autowired
    private StateMachine<OrderState, OrderEvent> stateMachine;

    @Autowired
    private OrderRepository orderRepository;

    private Map<String, Order> sagaStore = new ConcurrentHashMap<>();

    public void startSaga(Order order) {
        sagaStore.put(order.getOrderId(), order);
        stateMachine.getExtendedState().getVariables().put("order", order);
        stateMachine.start();
        stateMachine.sendEvent(OrderEvent.START_ORDER);
    }

    public void handlePaymentEvent(PaymentProcessedEvent event) {
        Order data = sagaStore.get(event.getOrderId());
        if (data == null) return;

        if ("SUCCESS".equals(event.getStatus())) {
            data.setPaymentTransactionId(event.getTransactionId());
            stateMachine.sendEvent(OrderEvent.PAYMENT_SUCCESS);
        } else {
            data.setErrorMessage(event.getErrorMessage());
            stateMachine.sendEvent(OrderEvent.PAYMENT_FAILED);
        }
    }

    public void handleInventoryEvent(InventoryReservedEvent event) {
        Order data = sagaStore.get(event.getOrderId());
        if (data == null) return;

        if ("SUCCESS".equals(event.getStatus())) {
            data.setInventoryTransactionId(event.getReservationId());
            stateMachine.sendEvent(OrderEvent.INVENTORY_SUCCESS);
        } else {
            data.setErrorMessage(event.getErrorMessage());
            stateMachine.sendEvent(OrderEvent.INVENTORY_FAILED);
        }
    }

    public void handleShippingEvent(ShipmentCreatedEvent event) {
        Order order = sagaStore.get(event.getOrderId());
        if (order == null) return;

        if ("SUCCESS".equals(event.getStatus())) {
            order.setShippingTransactionId(event.getShipmentId());
            stateMachine.sendEvent(OrderEvent.SHIPPING_SUCCESS);
            updateOrderStatus(event.getOrderId(), OrderState.ORDER_COMPLETED);
        } else {
            order.setErrorMessage(event.getErrorMessage());
            stateMachine.sendEvent(OrderEvent.SHIPPING_FAILED);
        }
    }

    public void handlePaymentRefundEvent(PaymentRefundedEvent event) {
        stateMachine.sendEvent(OrderEvent.COMPENSATE_PAYMENT);
        updateOrderStatus(event.getOrderId(), OrderState.ORDER_FAILED);
    }

    public void handleInventoryReleaseEvent(InventoryReleasedEvent event) {
        stateMachine.sendEvent(OrderEvent.COMPENSATE_INVENTORY);
    }

    private void updateOrderStatus(String orderId, OrderState status) {
        OrderEntity order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setState(status);
            orderRepository.save(order);
        }
    }
}
