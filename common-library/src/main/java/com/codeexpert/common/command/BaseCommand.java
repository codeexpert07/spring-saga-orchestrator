package com.codeexpert.common.command;

import com.codeexpert.common.event.DomainEvent;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseCommand implements DomainEvent {

    private String orderId;

    private String correlationId;

    private String status;

    private String errorMessage;
}
