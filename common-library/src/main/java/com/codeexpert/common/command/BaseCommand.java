package com.codeexpert.common.command;

import com.codeexpert.common.event.DomainEvent; // Added import
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class BaseCommand implements DomainEvent { // Implements DomainEvent

    private String orderId;

    private String correlationId;

    private String status;

    private String errorMessage;
}
