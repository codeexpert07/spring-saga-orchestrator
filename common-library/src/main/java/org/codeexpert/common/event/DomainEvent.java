package org.codeexpert.common.event;

import org.codeexpert.common.command.*;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
        @JsonSubTypes.Type(value = ReserveInventoryCommand.class, name = "ReserveInventoryCommand"),
        @JsonSubTypes.Type(value = InventoryReservedEvent.class, name = "InventoryReservedEvent"),
        @JsonSubTypes.Type(value = ShipmentCreatedEvent.class, name = "ShipmentCreatedEvent"),
        @JsonSubTypes.Type(value = InventoryReleasedEvent.class, name = "InventoryReleasedEvent"),
        @JsonSubTypes.Type(value = PaymentProcessedEvent.class, name = "PaymentProcessedEvent"),
})
public interface DomainEvent {
}
