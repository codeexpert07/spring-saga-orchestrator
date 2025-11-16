package org.codeexpert.order.service;

import org.codeexpert.common.command.*;
import org.codeexpert.common.constant.KafkaTopics;
import org.codeexpert.common.publisher.MessagePublisher; // Changed import
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class OrderCommandPublisher {

    @Autowired
    private MessagePublisher messagePublisher; // Changed injected type

    public void publishPaymentCommand(ProcessPaymentCommand command) {
        log.debug("Publishing payment command: {}", command);
        messagePublisher.publish(KafkaTopics.PAYMENT_COMMANDS, command.getOrderId(), command); // Changed commandPublisher to messagePublisher
    }

    public void publishRefundCommand(RefundPaymentCommand command) {
        log.debug("Publishing refund command: {}", command);
        messagePublisher.publish(KafkaTopics.PAYMENT_COMMANDS, command.getOrderId(), command); // Changed commandPublisher to messagePublisher
    }

    public void publishInventoryCommand(ReserveInventoryCommand command) {
        log.debug("Publishing inventory command: {}", command);
        messagePublisher.publish(KafkaTopics.INVENTORY_COMMANDS, command.getOrderId(), command); // Changed commandPublisher to messagePublisher
    }

    public void publishReleaseInventoryCommand(ReleaseInventoryCommand command) {
        log.debug("Publishing release inventory command: {}", command);
        messagePublisher.publish(KafkaTopics.INVENTORY_COMMANDS, command.getOrderId(), command); // Changed commandPublisher to messagePublisher
    }

    public void publishShippingCommand(CreateShipmentCommand command) {
        log.debug("Publishing shipping command: {}", command);
        messagePublisher.publish(KafkaTopics.SHIPPING_COMMANDS, command.getOrderId(), command); // Changed commandPublisher to messagePublisher
    }
}
