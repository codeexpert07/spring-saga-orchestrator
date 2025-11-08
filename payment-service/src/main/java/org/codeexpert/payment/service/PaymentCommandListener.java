package org.codeexpert.payment.service;

import com.codeexpert.common.command.ProcessPaymentCommand;
import com.codeexpert.common.constant.KafkaTopics;
import com.codeexpert.common.event.DomainEvent;
import com.codeexpert.common.event.PaymentProcessedEvent;
import com.codeexpert.common.listener.DomainEventListener;
import com.codeexpert.common.publisher.MessagePublisher; // Changed import
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Log4j2
@Service
public class PaymentCommandListener implements DomainEventListener {

    private final MessagePublisher messagePublisher; // Changed injected type

    @Autowired
    public PaymentCommandListener(MessagePublisher messagePublisher) { // Changed injected type
        this.messagePublisher = messagePublisher;
    }

    @Override
    public void onEvent(DomainEvent event) {
        if (event instanceof ProcessPaymentCommand command) {
            handleProcessPaymentCommand(command);
        } else {
            log.warn("Received unexpected event type: {}", event.getClass().getSimpleName());
        }
    }

    private void handleProcessPaymentCommand(ProcessPaymentCommand processPaymentCommand) {
        try {
            log.debug("Payment Service: Processing payment for order {}", processPaymentCommand.getOrderId());
            PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                    .orderId(processPaymentCommand.getOrderId())
                    .transactionId(UUID.randomUUID().toString())
                    .status("SUCCESS")
                    .correlationId(processPaymentCommand.getCorrelationId())
                    .build();

            messagePublisher.publish(KafkaTopics.PAYMENT_EVENTS, event.getOrderId(), event); // Changed commandPublisher to messagePublisher
        } catch (Exception e) {
            log.error("Error while processing payment: {}", processPaymentCommand, e);
            PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                    .orderId(processPaymentCommand.getOrderId())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .correlationId(processPaymentCommand.getCorrelationId())
                    .build();

            messagePublisher.publish(KafkaTopics.PAYMENT_EVENTS, event.getOrderId(), event); // Changed commandPublisher to messagePublisher
        }
    }
}
