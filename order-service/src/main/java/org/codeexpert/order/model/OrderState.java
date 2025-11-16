package org.codeexpert.order.model;

public enum OrderState {

    PENDING,

    PAYMENT_PROCESSING,      // Local transaction in Payment DB

    PAYMENT_COMPLETED,       // Payment DB committed

    INVENTORY_RESERVING,     // Local transaction in Inventory DB

    INVENTORY_RESERVED,      // Inventory DB committed

    SHIPPING_PROCESSING,     // Local transaction in Shipping DB

    ORDER_COMPLETED,         // All distributed transactions completed

    PAYMENT_COMPENSATING,    // Undo payment transaction

    INVENTORY_COMPENSATING,  // Undo inventory transaction

    ORDER_FAILED;            // Distributed transaction failed
}
