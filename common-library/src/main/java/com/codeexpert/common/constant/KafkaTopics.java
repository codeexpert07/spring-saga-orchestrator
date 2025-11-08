package com.codeexpert.common.constant;

public final class KafkaTopics {

    private KafkaTopics() {
        // Prevent instantiation
    }

    // Command Topics
    public static final String PAYMENT_COMMANDS = "payment-commands";
    public static final String INVENTORY_COMMANDS = "inventory-commands";
    public static final String SHIPPING_COMMANDS = "shipping-commands";

    // Event Topics
    public static final String PAYMENT_EVENTS = "payment-events";
    public static final String INVENTORY_EVENTS = "inventory-events";
    public static final String SHIPPING_EVENTS = "shipping-events";
}
