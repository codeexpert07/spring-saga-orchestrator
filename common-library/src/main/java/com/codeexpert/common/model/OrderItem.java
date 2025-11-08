package com.codeexpert.common.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItem {
    private String productId;
    private int quantity;
    private BigDecimal price;
}
