package org.codeexperts.order.config;

import com.codeexpert.common.command.*;
import org.codeexperts.order.service.OrderCommandPublisher;
import org.codeexperts.order.model.Order;
import org.codeexperts.order.model.OrderEvent;
import org.codeexperts.order.model.OrderState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

@Configuration
public class OrderStateMachineConfig extends StateMachineConfigurerAdapter<OrderState, OrderEvent> {

    @Autowired
    private OrderCommandPublisher commandPublisher;

    /**
     * Configure all the states for the order.
     *
     * @param states states configuration injected from Spring framework
     * @throws Exception if any
     */
    @Override
    public void configure(StateMachineStateConfigurer<OrderState, OrderEvent> states) throws Exception {
        states
                .withStates()
                .initial(OrderState.PENDING)
                .state(OrderState.PAYMENT_PROCESSING)
                .state(OrderState.PAYMENT_COMPLETED)
                .state(OrderState.INVENTORY_RESERVING)
                .state(OrderState.INVENTORY_RESERVED)
                .state(OrderState.SHIPPING_PROCESSING)
                .state(OrderState.PAYMENT_COMPENSATING)
                .state(OrderState.INVENTORY_COMPENSATING)
                .end(OrderState.ORDER_COMPLETED)
                .end(OrderState.ORDER_FAILED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderState, OrderEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(OrderState.PENDING)
                .target(OrderState.PAYMENT_PROCESSING)
                .event(OrderEvent.START_ORDER)
                .action(processPaymentAction())

                .and()
                .withExternal()
                .source(OrderState.PAYMENT_PROCESSING)
                .target(OrderState.PAYMENT_COMPLETED)
                .event(OrderEvent.PAYMENT_SUCCESS)
                .action(reserveInventoryAction())

                .and()
                .withExternal()
                .source(OrderState.PAYMENT_COMPLETED)
                .target(OrderState.INVENTORY_RESERVED)
                .event(OrderEvent.INVENTORY_SUCCESS)
                .action(processShippingAction())

                .and()
                .withExternal()
                .source(OrderState.INVENTORY_RESERVED)
                .target(OrderState.ORDER_COMPLETED)
                .event(OrderEvent.SHIPPING_SUCCESS)

                .and()
                .withExternal()
                .source(OrderState.PAYMENT_PROCESSING)
                .target(OrderState.ORDER_FAILED)
                .event(OrderEvent.PAYMENT_FAILED)

                .and()
                .withExternal()
                .source(OrderState.PAYMENT_COMPLETED)
                .target(OrderState.PAYMENT_COMPENSATING)
                .event(OrderEvent.INVENTORY_FAILED)
                .action(compensatePaymentAction())

                .and()
                .withExternal()
                .source(OrderState.PAYMENT_COMPENSATING)
                .target(OrderState.ORDER_FAILED)
                .event(OrderEvent.COMPENSATE_PAYMENT)

                .and()
                .withExternal()
                .source(OrderState.INVENTORY_RESERVED)
                .target(OrderState.INVENTORY_COMPENSATING)
                .event(OrderEvent.SHIPPING_FAILED)
                .action(compensateInventoryAction())

                .and()
                .withExternal()
                .source(OrderState.INVENTORY_COMPENSATING)
                .target(OrderState.PAYMENT_COMPENSATING)
                .event(OrderEvent.COMPENSATE_INVENTORY)
                .action(compensatePaymentAction());
    }

    @Bean
    public Action<OrderState, OrderEvent> processPaymentAction() {
        return context -> {
            Order data = context.getExtendedState().get("order", Order.class);

            ProcessPaymentCommand command = ProcessPaymentCommand.builder()
                    .orderId(data.getOrderId())
                    .customerId(data.getCustomerId())
                    .amount(data.getAmount())
                    .correlationId(data.getOrderId())
                    .build();

            commandPublisher.publishPaymentCommand(command);
        };
    }

    @Bean
    public Action<OrderState, OrderEvent> reserveInventoryAction() {
        return context -> {
            Order data = context.getExtendedState().get("order", Order.class);

            ReserveInventoryCommand command = ReserveInventoryCommand.builder()
                    .orderId(data.getOrderId())
                    .items(data.getItems())
                    .correlationId(data.getOrderId())
                    .build();

            commandPublisher.publishInventoryCommand(command);
        };
    }

    @Bean
    public Action<OrderState, OrderEvent> processShippingAction() {
        return context -> {
            Order data = context.getExtendedState().get("order", Order.class);

            CreateShipmentCommand command = CreateShipmentCommand.builder()
                    .orderId(data.getOrderId())
                    .items(data.getItems())
                    .correlationId(data.getOrderId())
                    .build();

            commandPublisher.publishShippingCommand(command);
        };
    }

    @Bean
    public Action<OrderState, OrderEvent> compensatePaymentAction() {
        return context -> {
            Order data = context.getExtendedState().get("order", Order.class);

            RefundPaymentCommand command = RefundPaymentCommand.builder()
                    .orderId(data.getOrderId())
                    .paymentTransactionId(data.getPaymentTransactionId())
                    .correlationId(data.getOrderId())
                    .build();

            commandPublisher.publishRefundCommand(command);
        };
    }

    @Bean
    public Action<OrderState, OrderEvent> compensateInventoryAction() {
        return context -> {
            Order data = context.getExtendedState().get("order", Order.class);

            ReleaseInventoryCommand command = ReleaseInventoryCommand.builder()
                    .orderId(data.getOrderId())
                    .reservationId(data.getInventoryTransactionId())
                    .correlationId(data.getOrderId())
                    .build();

            commandPublisher.publishReleaseInventoryCommand(command);
        };
    }
}
