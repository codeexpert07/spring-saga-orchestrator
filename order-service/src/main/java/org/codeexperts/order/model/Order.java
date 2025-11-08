package org.codeexperts.order.model;

import com.codeexpert.common.model.OrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Order {
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private List<OrderItem> items;

    // Transaction IDs for each distributed service
    private String paymentTransactionId;
    private String inventoryTransactionId;
    private String shippingTransactionId;

    private String errorMessage;
    private long startTime;
}
