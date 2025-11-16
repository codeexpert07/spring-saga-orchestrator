package org.codeexpert.order.controller;

import jakarta.validation.Valid;
import org.codeexpert.order.model.Order;
import org.codeexpert.order.service.OrderOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderOrchestrator orderOrchestrator;

    public OrderController(OrderOrchestrator orderOrchestrator) {
        this.orderOrchestrator = orderOrchestrator;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody OrderRequest request) {
        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());
        order.setCustomerId(request.getCustomerId());
        order.setAmount(new BigDecimal(request.getAmount()));
        order.setItems(request.getItems());
        order.setStartTime(System.currentTimeMillis());

        // Start the order processing saga
        orderOrchestrator.startSaga(order);

        return ResponseEntity.accepted().body(order);
    }

    // Request DTO for order creation
    public static class OrderRequest {
        private String customerId;
        private String amount;
        private java.util.List<org.codeexpert.common.model.OrderItem> items;

        // Getters and Setters
        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public java.util.List<org.codeexpert.common.model.OrderItem> getItems() {
            return items;
        }

        public void setItems(java.util.List<org.codeexpert.common.model.OrderItem> items) {
            this.items = items;
        }
    }
}
