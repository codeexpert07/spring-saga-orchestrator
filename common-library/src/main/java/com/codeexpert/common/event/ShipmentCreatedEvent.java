package com.codeexpert.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ShipmentCreatedEvent extends BaseEvent { // Extends BaseEvent

    private String shipmentId;
}
