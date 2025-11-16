package org.codeexpert.order.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.codeexpert.order.model.OrderState;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Builder
@Table(name = "ORDERS")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;
    private String customerId;
    private BigDecimal amount;
    private String productId;
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private OrderState state;
}
