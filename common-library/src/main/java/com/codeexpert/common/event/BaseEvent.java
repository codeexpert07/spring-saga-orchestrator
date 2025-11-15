package com.codeexpert.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseEvent implements DomainEvent {

    private String orderId;

    private String correlationId;

    private String status;

    private String errorMessage;
}
