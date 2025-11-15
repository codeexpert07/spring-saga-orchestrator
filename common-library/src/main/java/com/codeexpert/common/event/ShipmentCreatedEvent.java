package com.codeexpert.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentCreatedEvent extends BaseEvent { // Extends BaseEvent

    @Builder.Default
    private String commandType = "ShipmentCreatedEvent";

    private String shipmentId;
}
