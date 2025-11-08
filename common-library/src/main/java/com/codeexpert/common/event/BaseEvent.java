package com.codeexpert.common.event;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class BaseEvent implements DomainEvent {

    private String orderId;

    private String correlationId;

    private String status;

    private String errorMessage;
}
