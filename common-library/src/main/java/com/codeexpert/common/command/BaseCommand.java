package com.codeexpert.common.command;

import com.codeexpert.common.event.DomainEvent;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "commandType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreateShipmentCommand.class, name = "CreateShipmentCommand"),
        @JsonSubTypes.Type(value = ProcessPaymentCommand.class, name = "ProcessPaymentCommand"),
        @JsonSubTypes.Type(value = RefundPaymentCommand.class, name = "RefundPaymentCommand"),
        @JsonSubTypes.Type(value = ReleaseInventoryCommand.class, name = "ReleaseInventoryCommand"),
        @JsonSubTypes.Type(value = ReserveInventoryCommand.class, name = "ReserveInventoryCommand")
})
public class BaseCommand implements DomainEvent {

    private String orderId;

    private String correlationId;

    private String status;

    private String errorMessage;
}
