package org.codeexpert.common.command;

import org.codeexpert.common.event.DomainEvent;
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
